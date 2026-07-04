# FleetMgm
This is a fleet management solution. Backend: Java 21 + Spring Boot 3.5. Frontend: React + Vite + TypeScript, recycling components in a mobile app. Technology decisions and rationale are defined in [`planning.md`](planning.md).

## Local Development Setup

### NVD API key (OWASP Dependency-Check)

`backend/pom.xml` runs `org.owasp:dependency-check-maven`, which downloads CVE data from NVD. Without an API key, NVD rate-limits requests heavily and the first download can take a very long time.

1. Request a free key: https://nvd.nist.gov/developers/request-an-api-key
2. Store it in `.env` at the repo root (already gitignored):
   ```
   NVD_API_KEY=your-key-here
   ```
3. Every new PowerShell session, load it into the environment **before** running Maven (the variable does not persist across terminal restarts):
   ```powershell
   cd backend
   $env:NVD_API_KEY = (Get-Content ..\.env | Select-String '^NVD_API_KEY=(.*)').Matches.Groups[1].Value
   ```
4. Verify it loaded (should print your key, not blank):
   ```powershell
   $env:NVD_API_KEY
   ```
5. Run the check:
   ```powershell
   .\mvnw.cmd dependency-check:check
   ```

CI reads the same variable from the `NVD_API_KEY` GitHub Actions secret (already configured) — no `.env` needed there.

