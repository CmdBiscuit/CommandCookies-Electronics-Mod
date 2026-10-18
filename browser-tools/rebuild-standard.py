"""Compile the Standard helper without altering shipped files; verify class parity."""
from pathlib import Path
import json, os, shutil, subprocess, zipfile

root = Path(__file__).resolve().parents[1]
browser = root / 'main/src/main/resources/assets/commandcookies_electronic_mod/browser'
output = root / 'build/browser-tools/standard-classes'
output.mkdir(parents=True, exist_ok=True)
java_home = os.environ.get('JAVA_HOME')
javac = str(Path(java_home) / 'bin/javac') if java_home else shutil.which('javac')
if not javac:
    raise SystemExit('Set JAVA_HOME to a Java 21 JDK, or add javac to PATH.')
if os.name == 'nt' and not javac.endswith('.exe'):
    javac += '.exe'
names = ['jcef-api', 'jcefmaven', 'gson', 'commons-compress', 'commons-io', 'commons-lang3']
classpath = os.pathsep.join(str(browser / 'libs' / (n + '.jar')) for n in names)
sources = sorted((root / 'browser-tools/standard/src').rglob('*.java'))
subprocess.run([javac, '--release', '21', '-encoding', 'UTF-8', '-cp', classpath, '-d', str(output), *map(str, sources)], check=True)
classes = {p.relative_to(output).as_posix(): p.read_bytes() for p in output.rglob('*.class')}
with zipfile.ZipFile(browser / 'libs/cem-browser-helper.jar') as packaged:
    expected = {n: packaged.read(n) for n in packaged.namelist() if n.endswith('.class')}
differences = sorted(n for n in classes.keys() | expected.keys() if classes.get(n) != expected.get(n))
result = {'status': 'PASS' if not differences else 'DIFFERENT', 'classes': len(classes), 'differences': differences}
(root / 'build/browser-tools/standard-verification.json').write_text(json.dumps(result, indent=2), encoding='utf8')
print(json.dumps(result))
raise SystemExit(0 if not differences else 1)
