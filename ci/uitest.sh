#!/usr/bin/env bash
# Emulator pe app chala ke ci/steps.txt ke hisaab se tap/type karta hai, har "shot" pe screenshot leta hai
# aur crash log nikalta hai. Output: out/ folder (CI isko ci-screens branch pe daal deta hai).
set -u
PKG="$1"; APK="$2"; STEPS="${3:-ci/steps.txt}"
mkdir -p out
adb install -r -g "$APK" || { echo "install failed" > out/RESULT.txt; exit 0; }
adb logcat -c
adb shell settings put global window_animation_scale 0
adb shell settings put global transition_animation_scale 0
adb shell settings put global animator_duration_scale 0

find_node() {  # $1 = text ya content-desc; center "x y" print karta hai
  rm -f /tmp/ui.xml
  for i in 1 2 3; do
    adb shell uiautomator dump /sdcard/ui.xml >/dev/null 2>&1 && adb pull /sdcard/ui.xml /tmp/ui.xml >/dev/null 2>&1 && break
    sleep 1
  done
  python3 - "$1" <<'PY'
import sys, re, xml.etree.ElementTree as ET
want = sys.argv[1]
try: nodes = list(ET.parse('/tmp/ui.xml').iter('node'))
except Exception: sys.exit(1)
def c(n):
    x1,y1,x2,y2 = map(int, re.findall(r'\d+', n.get('bounds')))
    return f"{(x1+x2)//2} {(y1+y2)//2}"
for exact in (True, False):
    for n in nodes:
        for k in ('text','content-desc'):
            v = n.get(k) or ''
            if (v == want) if exact else (want.lower() in v.lower() and v):
                print(c(n)); sys.exit(0)
sys.exit(1)
PY
}

n=0
while IFS= read -r -u 3 line || [ -n "$line" ]; do
  [ -z "$line" ] && continue; case "$line" in \#*) continue;; esac
  cmd="${line%% *}"; arg="${line#* }"
  echo ">> $line"
  case "$cmd" in
    launch) adb shell am start -n "$PKG/.MainActivity" --ez ci_no_ads true >/dev/null 2>&1 ;;
    wait) sleep "$arg" ;;
    shot) n=$((n+1)); adb exec-out screencap -p > "out/$(printf %02d $n)_$arg.png" ;;
    tap) if xy=$(find_node "$arg"); then adb shell input tap $xy; else
           { echo "(not found: $arg) screen texts:"; grep -oE '(text|content-desc)="[^"]+"' /tmp/ui.xml 2>/dev/null | head -25 || echo "  no ui dump"; } >> out/missing.txt; fi ;;
    tapxy) adb shell input tap $arg ;;
    type) adb shell input text "${arg// /%s}" ;;
    key) adb shell input keyevent "$arg" ;;
    swipe) adb shell input swipe $arg ;;
  esac
  sleep 1
  if adb logcat -d -b crash | grep -q "FATAL EXCEPTION"; then
    n=$((n+1)); adb exec-out screencap -p > "out/$(printf %02d $n)_CRASH.png"; break
  fi
done 3< "$STEPS"

adb logcat -d -b crash > out/crash.txt
adb logcat -d | grep -E "ActivityManager|ActivityTaskManager|AndroidRuntime|$PKG|System.err" | tail -400 > out/errors.txt
if grep -q "FATAL EXCEPTION" out/crash.txt; then echo "CRASH" > out/RESULT.txt; else echo "OK" > out/RESULT.txt; fi
cat out/RESULT.txt
