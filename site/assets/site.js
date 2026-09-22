const tabs = [...document.querySelectorAll('[role="tab"]')];
const copyButton = document.querySelector('.copy-button');
const status = document.querySelector('.copy-status');
let copyReset;

function selectTab(tab, focus = false) {
  tabs.forEach((item) => {
    const selected = item === tab;
    item.setAttribute('aria-selected', String(selected));
    item.tabIndex = selected ? 0 : -1;
    document.getElementById(item.getAttribute('aria-controls')).hidden = !selected;
  });
  copyButton.setAttribute('aria-label', `Copy ${tab.textContent} setup commands`);
  status.textContent = '';
  copyButton.querySelector('span').textContent = 'Copy';
  clearTimeout(copyReset);
  if (focus) tab.focus();
}

tabs.forEach((tab, index) => {
  tab.addEventListener('click', () => selectTab(tab));
  tab.addEventListener('keydown', (event) => {
    let next;
    if (event.key === 'ArrowRight') next = (index + 1) % tabs.length;
    if (event.key === 'ArrowLeft') next = (index + tabs.length - 1) % tabs.length;
    if (event.key === 'Home') next = 0;
    if (event.key === 'End') next = tabs.length - 1;
    if (next !== undefined) {
      event.preventDefault();
      selectTab(tabs[next], true);
    }
  });
});

copyButton.addEventListener('click', async () => {
  const panel = document.querySelector('.terminal-panel:not([hidden])');
  const code = panel.querySelector('code');
  const platform = tabs.find((tab) => tab.getAttribute('aria-selected') === 'true').textContent;
  try {
    await navigator.clipboard.writeText(code.textContent.trim());
    status.textContent = `${platform} commands copied.`;
    copyButton.querySelector('span').textContent = 'Copied';
    clearTimeout(copyReset);
    copyReset = setTimeout(() => {
      copyButton.querySelector('span').textContent = 'Copy';
      status.textContent = '';
    }, 4000);
  } catch {
    const selection = window.getSelection();
    const range = document.createRange();
    range.selectNodeContents(code);
    selection.removeAllRanges();
    selection.addRange(range);
    panel.focus();
    status.textContent = 'Commands selected. Use your browser’s Copy command.';
  }
});
