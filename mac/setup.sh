mkdir -p ~/MacDeck && cd ~/MacDeck
[ -f pin.txt ] || echo 1234 > pin.txt

cat > inject.js <<'EOF'
ObjC.import('Cocoa');
ObjC.import('ApplicationServices');
function run(argv) {
  var pin = argv[0] || '1234';
  var inp = $.NSFileHandle.fileHandleWithStandardInput;
  var out = $.NSFileHandle.fileHandleWithStandardOutput;
  function say(s) { out.writeData($(s + '\n').dataUsingEncoding($.NSUTF8StringEncoding)); }
  function tap(code, flags) {
    [true, false].forEach(function (down) {
      var ev = $.CGEventCreateKeyboardEvent($(), code, down);
      $.CGEventSetFlags(ev, flags);
      $.CGEventPost($.kCGHIDEventTap, ev);
    });
  }
  var buf = '', authed = false;
  while (true) {
    var data = inp.availableData;
    if (data.length === 0) break;
    buf += ObjC.unwrap($.NSString.alloc.initWithDataEncoding(data, $.NSUTF8StringEncoding));
    var i;
    while ((i = buf.indexOf('\n')) >= 0) {
      var p = buf.slice(0, i).trim().split(' ');
      buf = buf.slice(i + 1);
      if (!authed) {
        if (p[0] === 'HELLO' && p[1] === pin) { authed = true; say('OK'); } else { say('NO'); return; }
      } else if (p[0] === 'K') {
        tap(parseInt(p[1], 10), parseInt(p[2], 10) || 0);
      }
    }
  }
}
EOF

cat > start.sh <<'EOF'
#!/bin/bash
cd "$(dirname "$0")"
PORT=8765
PIN=$(tr -d ' \n' < pin.txt)
ADB=$(command -v adb || echo "$PWD/platform-tools/adb")
IP=$(for i in en0 en1 en2 en3; do ipconfig getifaddr $i 2>/dev/null && break; done)
clear
echo "MacDeck is running"
echo "Wi-Fi mode: IP $IP   port $PORT   PIN $PIN"
echo "USB mode:   plug in the tablet (USB debugging on)"
echo "Close this window to stop."
F=/tmp/macdeck.fifo; rm -f $F; mkfifo $F
trap 'rm -f $F; exit' INT TERM HUP
while true; do
  [ -x "$ADB" ] && "$ADB" reverse tcp:$PORT tcp:$PORT >/dev/null 2>&1
  nc -l $PORT < $F | osascript -l JavaScript inject.js "$PIN" > $F
  sleep 0.2
done
EOF
chmod +x start.sh

printf '#!/bin/bash\nexec ~/MacDeck/start.sh\n' > ~/Desktop/MacDeck.command
chmod +x ~/Desktop/MacDeck.command

# USB mode only: adb is just an unzipped folder, no installer
if ! command -v adb >/dev/null && [ ! -x platform-tools/adb ]; then
  curl -sLo pt.zip https://dl.google.com/android/repository/platform-tools-latest-darwin.zip && unzip -q pt.zip && rm -f pt.zip
fi
echo "Done. Double-click MacDeck.command on your Desktop."
