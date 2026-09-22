# Bundled card catalog

Seven English Pokémon TCG cards from Scarlet & Violet—151 support this offline example. They are the same artwork files used in the collection Figma designs. They are sample catalog data, not pre-populated personal ownership.

Metadata and card images were retrieved from [TCGdex](https://www.tcgdex.net/). Each original image follows `https://assets.tcgdex.net/en/sv/sv03.5/<number>/high.png`; metadata follows `https://api.tcgdex.net/v2/en/cards/sv03.5-<number>`.

| Printed number | Card | Bundled image |
| --- | --- | --- |
| 025 | Pikachu | `card_025.png` |
| 001 | Bulbasaur | `card_001.png` |
| 004 | Charmander | `card_004.png` |
| 007 | Squirtle | `card_007.png` |
| 199 | Charizard ex | `card_199.png` |
| 200 | Blastoise ex | `card_200.png` |
| 205 | Mew ex | `card_205.png` |

Pokémon and Pokémon TCG card artwork belong to their respective rights holders, including The Pokémon Company, Nintendo, GAME FREAK, and Creatures. This is an unofficial educational example, not an affiliated Pokémon product. Artwork is not relicensed by this repository.

`catalog.json` is the common static resource contract; each native build packages this directory independently. The apps make no network requests for card data or images. Normal/Reverse holo are available for the four common cards; the three ex cards use Holo. Runtime catalog counts derive from this file.
