# OpenPGP Business Cards

Generate business cards with OpenPGP fingerprints.

## Idea
Templates are designed as scalable vector graphics with a program such as inkscape.
Key values in the templates are replaced by values extracted from GnuPG keys.

## Usage
Run `makecard -h` for a description of available options.

## Example
See `dist` folder for examples.

## Dependencies
  - GnuPG
  - qrencode
  - *optional* inkscape to convert SVGs to PDFs