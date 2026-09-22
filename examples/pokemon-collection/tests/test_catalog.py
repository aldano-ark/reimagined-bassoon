"""Validate the static data both native builds consume, without native tools."""
import json
from pathlib import Path
import struct
import unittest


ASSETS = Path(__file__).resolve().parents[1] / "assets"


class CatalogTests(unittest.TestCase):
    @classmethod
    def setUpClass(cls):
        cls.cards = json.loads((ASSETS / "catalog.json").read_text())

    def test_stable_unique_ids_match_printed_numbers(self):
        self.assertEqual(len(self.cards), 7)
        self.assertEqual(len({card["id"] for card in self.cards}), 7)
        for card in self.cards:
            with self.subTest(card=card["id"]):
                self.assertRegex(card["number"], r"^\d{3}$")
                self.assertEqual(card["id"], f"sv03.5-{card['number']}")
                self.assertEqual(card["set"], "151")
                self.assertEqual(card["printedTotal"], 165)
                self.assertTrue(card["name"].strip())
                self.assertTrue(card["rarity"].strip())

    def test_every_card_has_a_safe_local_full_card_png(self):
        for card in self.cards:
            with self.subTest(card=card["id"]):
                image = card["image"]
                self.assertEqual(image, f"card_{card['number']}.png")
                data = (ASSETS / image).read_bytes()
                self.assertEqual(data[:8], b"\x89PNG\r\n\x1a\n")
                self.assertEqual(data[12:16], b"IHDR")
                width, height = struct.unpack(">II", data[16:24])
                self.assertGreaterEqual(width, 200)
                self.assertGreaterEqual(height, 280)
                self.assertAlmostEqual(width / height, 0.716, delta=0.025)

    def test_finishes_are_card_specific(self):
        for card in self.cards:
            with self.subTest(card=card["id"]):
                expected = ["Normal", "Reverse holo"] if int(card["number"]) < 165 else ["Holo"]
                self.assertEqual(card["finishes"], expected)

    def test_catalog_contains_no_fabricated_user_state_or_prices(self):
        expected_keys = {"id", "name", "number", "set", "printedTotal", "rarity", "image", "finishes"}
        for card in self.cards:
            self.assertEqual(set(card), expected_keys)


if __name__ == "__main__":
    unittest.main()
