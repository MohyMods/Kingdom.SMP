# Kingdom SMP 0.13.0-alpha

Minecraft Java **1.21.1**, **NeoForge 21.1.265**, **Java 21**. Install the same Kingdom SMP JAR and matching dependency versions on server and all clients. Do not use Fabric/Forge versions.

Back up the complete world before upgrading. Stop the server, remove the older Kingdom SMP JAR, then install this JAR in `mods/`. Keep the world's `data/`, `playerdata/`, `serverconfig/`, claims and mod configs together. Never transplant individual economy/player files between saves. Existing Lodge-profile worlds retain their Lodge, Mira and records; fresh worlds use the Town Hall profile. No automatic building replacement occurs.

The dependency manifest pins the **29-artifact compatibility profile actually tested**, with official URLs, file sizes and SHA-256 hashes. It is not certification of the owner's entire 277-mod pack. The bundled downloader uses Python 3 and refuses mismatched or unexpected JARs; download into an empty directory, then deliberately install the matching files. Third-party JARs are not redistributed in this ZIP. Read and comply with their respective licenses. Avoid installing duplicate versions from an existing pack.

For a fresh installation, download the dependencies using `python3 scripts/download-alpha-mods.py --destination alpha-mods`, copy those JARs plus the Kingdom SMP JAR into `mods/`, and start with Java 21/NeoForge 21.1.265. Normal server EULA acceptance and NeoForge installation remain your responsibility.

World configuration files are generated safely. Do not overwrite generated world configs with test fixture settings. No QA saves, enabled sandbox, artificial balances, accelerated contract periods or test-only source proofs belong in a production installation.

Scope: Tier 1–6 and Farmer, Blacksmith, Builder, Lumberjack, Miner and Librarian. Existing lower-rank content of other roles is preserved. Unfinished/deferred roles and Tier 7–12 display Coming Soon. This is an Alpha, not the complete 1.0 game.
