# Demo manuals

Run `python scripts/prepare_manuals.py manuals` from the repository root to regenerate all runtime document assets.

- `db200h.pdf`: Hoshizaki DB-200H installation manual, supplied by the user. Source: https://secure.hoshizakiamerica.com/docs/manuals/DB-200H_inst.pdf
- `mlg202dr.pdf`: American Dryer Corporation AD-202 / MLG-202DR installation manual, revision 113289-6, 66 PDF pages including ManualsLib front matter. Source: https://www.manualslib.com/manual/2788481/American-Dryer-Corp-Ad-202.html
- `vsx.pdf`: Bell & Gossett Series VSX manual P5002169, supplied as 34778383.pdf. The demo user alias is P500219; the document covers VSH/VSC/VSCS variants.

PDF page positions are preserved. The preparation script records SHA-256 hashes and maps printed page labels. Manufacturer copyright and notices remain in the original files.
