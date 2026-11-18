# Publication status

The Alpha is built and tested; source and release documentation are pushed to `rebuild/royal-gilded`. Public release-asset delivery is blocked by external authentication, not a build failure.

An unpublished GitHub pre-release draft exists: release ID `449518920`, requested tag `v0.13.0-alpha`, target `b5d74f072e45aa19f9c3b845bfda3eda2448d652`, with no uploaded assets.

Owner draft: https://github.com/MohyMods/Kingdom.SMP/releases/tag/untagged-be2867b2efb84eb9c6de

GitHub API authentication identifies MohyMods and repository writes work. Both the normal GitHub CLI upload and a standard explicitly authenticated HTTPS upload to the returned official `uploads.github.com` endpoint fail with HTTP401 Bad credentials. The connected environment is running with unrestricted enforced networking; this is not an HTTP403 dependency-download problem. No credential values are recorded here. Do not publish the empty draft or describe its assets as publicly downloadable.

Verified local delivery files are under `/workspace/Kingdom.SMP/dist/0.13.0-alpha/`: installable `kingdom-smp-core-0.13.0-alpha.jar`, `Kingdom-SMP-0.13.0-alpha-install.zip`, and `SHA256SUMS.txt`. The JAR hash is `3b582c5e7f57becf91ca94a683b072882fd39ca54990d67214205dd3b65f88b1`. The ZIP checksum is supplied alongside the current ZIP. Cloud desktop clients can save the linked files locally.

After restoring a credential accepted for release-asset uploads, inspect the existing draft rather than creating duplicates, upload all three verified files, compare their names/sizes/hashes, and only then publish it as a pre-release. The owner may alternatively upload these files directly in GitHub's draft UI. No gameplay work or repeated build is needed to resume publication. If the source branch receives documentation-only follow-up commits, preserve the tested implementation947cb0a; do not move the release to untested gameplay changes.
