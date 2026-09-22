"""Exercise real subprocess cleanup without touching Simulator or Xcode."""
import importlib.util
from pathlib import Path
import signal
import sys
import tempfile
import threading
import time
import unittest

SPEC = importlib.util.spec_from_file_location('runner', Path(__file__).resolve().parents[1] / 'runner.py')
runner = importlib.util.module_from_spec(SPEC)
SPEC.loader.exec_module(runner)


class CommandLifecycleTests(unittest.TestCase):
    def setUp(self):
        self.directory = tempfile.TemporaryDirectory()
        self.addCleanup(self.directory.cleanup)
        self.root = Path(self.directory.name)
        self.commands = runner.Commands(self.root, self.root)

    def command_with_descendant(self):
        # Surviving descendants would write evidence after their parent is stopped.
        child = "import time; from pathlib import Path; time.sleep(0.8); Path('escaped').write_text('survived')"
        parent = f'import subprocess,sys,time; subprocess.Popen([sys.executable, "-c", {child!r}]); time.sleep(30)'
        return [sys.executable, '-c', parent]

    def assert_no_descendant_escape(self):
        time.sleep(1)
        self.assertFalse((self.root / 'escaped').exists())

    def test_timeout_terminates_descendants_before_returning(self):
        with self.assertRaises(runner.RunFailure) as caught:
            self.commands.run(self.command_with_descendant(), 'timeout', timeout=0.25)
        self.assertEqual(caught.exception.status, 124)
        self.assert_no_descendant_escape()

    def test_signal_terminates_descendants_and_allows_cleanup(self):
        timer = threading.Timer(0.25, self.commands.on_signal, [signal.SIGTERM, None])
        timer.start()
        self.addCleanup(timer.cancel)
        with self.assertRaises(runner.RunFailure) as caught:
            self.commands.run(self.command_with_descendant(), 'signal')
        self.assertEqual(caught.exception.status, 143)
        self.commands.cleaning = True
        self.assertEqual(self.commands.run([sys.executable, '-c', 'print("cleanup")'], 'cleanup', capture=True).strip(), 'cleanup')
        self.assert_no_descendant_escape()

    def test_capture_and_nonzero_exit_preserve_status(self):
        self.assertEqual(self.commands.run([sys.executable, '-c', 'print("ready")'], 'capture', capture=True).strip(), 'ready')
        with self.assertRaises(runner.RunFailure) as caught:
            self.commands.run([sys.executable, '-c', 'raise SystemExit(7)'], 'failure')
        self.assertEqual(caught.exception.status, 7)


if __name__ == '__main__':
    unittest.main()
