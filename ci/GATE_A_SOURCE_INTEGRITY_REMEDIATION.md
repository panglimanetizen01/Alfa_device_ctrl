# Gate A Source Integrity Remediation

The canonical `master` source previously contained `.g1-loader-probe/proot` as a gitlink without a corresponding submodule definition. Gate A resolves every tracked path from the canonical commit with `git show`, so that orphaned gitlink caused `fatal: bad object`.

Commit `57aa0e6121688de9f1ec018872f750124ebd3ff2` removes the orphaned `.g1-loader-probe` tree. This file records the remediation checkpoint and intentionally contains no executable CI behavior.
