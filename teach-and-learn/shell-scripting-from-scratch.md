# Writing bash/zsh scripts from scratch — a full lesson

A tutorial, not a reference sheet. Goal: after reading this top to bottom,
you should be able to write something like `dev-up.sh` (the script in this
repo's root that starts both apps + Postgres) yourself, and understand every
line of it. We'll build up the concepts one at a time with tiny throwaway
examples, then read the real script at the end line by line.

You don't need to know any of this already. If a word is unfamiliar, keep
reading — it gets defined before it's used.

---

## 1. What even is a "script"?

A script is just a text file full of commands you'd otherwise type one at a
time into your terminal, saved so you can run them all at once by typing one
thing instead of many things.

Try this right now. Make a file called `hello.sh`:

```bash
#!/usr/bin/env bash
echo "Hello, world"
```

Two lines. Let's take them apart.

### The shebang (`#!/usr/bin/env bash`)

That first line is called a **shebang**. It's not a comment, even though it
starts with `#` — the operating system specifically looks for `#!` at the
very start of a file to figure out "what program should run this file?"

`#!/usr/bin/env bash` means: "find `bash` on this system (wherever it's
installed) and run this file as a bash script." You could also write
`#!/bin/bash` (assumes bash lives at exactly `/bin/bash`), but `/usr/bin/env
bash` is more portable — it works even if bash lives somewhere unusual.

Without a shebang, the computer has no idea what language the file is in.

### `echo`

`echo` just prints text to the screen. That's it. `echo "Hello, world"`
prints `Hello, world`.

### Running it

A file isn't runnable just because it has a shebang — you need to tell the
filesystem "this file is allowed to be executed," with `chmod`:

```bash
chmod +x hello.sh
./hello.sh
```

`chmod +x` = "change mode: add the executable permission." The `./` in
`./hello.sh` means "look for this file in the current folder" — without it,
your shell searches only folders listed in a system variable called `PATH`,
and won't find a script sitting right next to you unless you point at it
explicitly.

If you don't want to `chmod +x` it, you can also just run it as an argument
to bash directly: `bash hello.sh`. Same result, skips the permission step.

That's the whole shape of every script in this tutorial: shebang line, then
commands, run with `./name.sh`.

---

## 2. Variables

```bash
#!/usr/bin/env bash
name="Araf"
echo "Hello, $name"
```

- `name="Araf"` — **no spaces around the `=`**. `name = "Araf"` is a syntax
  error in bash (it looks like you're trying to run a command called `name`
  with arguments `=` and `"Araf"`). This trips up literally everyone once.
- `$name` reads the variable. Always quote variables you're using in a
  string or passing to a command — `"$name"` not `$name` — otherwise if the
  value ever contains a space, bash splits it into multiple words and
  chaos follows. Get in the habit now.

### Command substitution — capture a command's output into a variable

```bash
today=$(date +%Y-%m-%d)
echo "Today is $today"
```

`$( ... )` runs whatever's inside, and swaps in whatever it printed. This is
one of the most useful things in shell scripting — it's how you turn "run a
command" into "use its answer."

You'll see this exact pattern in `dev-up.sh`:

```bash
REPO="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
```

Reading inside-out: `${BASH_SOURCE[0]}` is "the path to this very script
file." `dirname` strips the filename off, leaving just the folder. `cd` into
that folder, then `pwd` prints the *absolute* path of wherever you just
`cd`'d. Net effect: `REPO` becomes the full absolute path of the folder this
script lives in — **regardless of what folder you were sitting in when you
ran it**. That matters because the alias `landlord-dev` might get typed from
anywhere on your computer, and the script still needs to find
`landlord-backend/`, `docker-compose.yml`, etc. relative to *itself*, not
relative to wherever your terminal happened to be.

---

## 3. Exit codes — how commands report success or failure

Every command, when it finishes, leaves behind a number called an **exit
code**. `0` means "succeeded." Anything else (1–255) means "something went
wrong," and different programs use different non-zero numbers to mean
different specific failures.

You almost never see this number unless you ask for it:

```bash
ls /tmp
echo "exit code was: $?"      # $? = the exit code of the last command run
```

Try `ls /this/does/not/exist` instead and check `$?` again — you'll get a
non-zero number.

This matters because `if` statements in shell don't check "true/false" the
way most languages do — **they check exit codes**. `if some_command; then`
means "if `some_command` exits with code 0, do the following."

### `if`, in practice

```bash
if curl -s http://127.0.0.1:8080 > /dev/null; then
  echo "something is listening on 8080"
else
  echo "nothing there"
fi
```

`curl` here just tries to fetch that URL. If it can connect at all (even to
get an error page), curl exits `0`. If it can't connect (nothing's
listening), curl exits non-zero. That's the entire mechanism `dev-up.sh`
uses to detect "is this service already running?" — no magic, just "did
this network request succeed or fail."

`> /dev/null` throws away curl's actual output because we only care about
*whether it worked*, not what it printed. `/dev/null` is a special file that
discards anything written to it — think of it as a trash can with no bottom.

### `set -e`, `set -u`, `set -o pipefail`

Near the top of most serious scripts you'll see:

```bash
set -euo pipefail
```

Three separate safety switches, usually combined:

- `set -e` — **stop the whole script immediately** if any command fails
  (non-zero exit code), instead of barreling on as if nothing happened. Plain
  bash's default is horrifyingly permissive — without this, a failed `cd`
  followed by `rm -rf *` would happily `rm -rf` your *current* folder instead
  of the one you meant to be in.
- `set -u` — treat using an **undefined variable** as an error, instead of
  silently treating it as an empty string. Saves you from typo bugs like
  `echo $nmae` doing nothing instead of loudly complaining.
- `set -o pipefail` — normally, a pipeline like `cmd1 | cmd2` only reports
  failure if the *last* command (`cmd2`) fails, even if `cmd1` blew up.
  `pipefail` makes the whole pipeline fail if *any* stage does.

Note: `dev-up.sh` actually uses `set -uo pipefail` — deliberately **without**
`-e`. Why? Because the script's whole job is to try four different services
and keep going even if one of them fails to start (so you find out about all
four problems, not just the first). `set -e` would kill the entire script
the moment the very first `curl` check returned non-zero. Knowing when
*not* to use a safety feature is as important as knowing it exists.

---

## 4. Functions

A function is a named, reusable block of commands — same idea as a function
in any programming language, just plainer syntax:

```bash
greet() {
  echo "Hello, $1"
}

greet "Araf"
greet "World"
```

- `greet()` defines it. The `{ }` braces hold the body.
- `$1` is the function's **first argument** — whatever was passed in when
  you called it. `$2` would be the second, and so on. There's no named-
  parameter syntax in bash; you just refer to arguments by position.
- No `return` needed for a function like this one — it does its thing and
  finishes. Functions *can* set an exit code with `return N`, which matters
  when a function is itself used inside an `if`.

Here's a function that returns success/failure rather than printing
something, straight out of `dev-up.sh`:

```bash
port_up() {
  local port="$1"
  local code
  code=$(curl -s -o /dev/null -w "%{http_code}" "http://127.0.0.1:${port}" 2>/dev/null)
  [ "$code" != "000" ]
}
```

New things here:

- `local port="$1"` — `local` means this variable only exists inside this
  function, not leaking out into the rest of the script. Good hygiene:
  without `local`, every variable in a bash script is global by default,
  which gets confusing fast once you have several functions.
- `curl -s -o /dev/null -w "%{http_code}" URL` — a very common curl idiom.
  `-s` = silent (don't print curl's own progress bar). `-o /dev/null` =
  throw away the actual response body, we don't care about the HTML/JSON.
  `-w "%{http_code}"` = *do* print the HTTP status code (200, 404, 401,
  etc.) after the request finishes. So this whole line means: "hit this
  URL, tell me only the status code, throw away everything else."
- `2>/dev/null` — redirect **stderr** (error messages) to the trash can too,
  so if curl can't even connect, its error text doesn't clutter your
  script's output. (More on the `2>` numbering in the redirection section
  below.)
- `[ "$code" != "000" ]` — this is the actual **test**. curl prints `000` as
  the status code specifically when it couldn't connect at all (nothing's
  listening on that port). So "status code is not 000" means "something
  answered." This one square-bracket line's exit code (0 or 1) becomes the
  function's own exit code — no explicit `return` needed, because in bash a
  function's exit code defaults to the exit code of the last command inside
  it.

The `[ ... ]` syntax deserves its own note: `[` is actually a *command*
(historically a real program on disk, `/usr/bin/[`), not special syntax —
that's why there must be a space after `[` and before `]`. `[ "$code" !=
"000" ]` means "test whether these two strings are not equal," and its exit
code is 0 (true/matched) or 1 (false/didn't match), same as everything else
in this section.

---

## 5. Loops

### `for`, over a fixed list

```bash
for name in landlord barivara; do
  echo "hello from $name"
done
```

### `for`, counting

```bash
for i in $(seq 1 5); do
  echo "attempt $i"
done
```

`seq 1 5` prints `1 2 3 4 5` (one per line); the `for` loop walks through
each one. This exact pattern is how `dev-up.sh` polls a service without
waiting forever:

```bash
wait_for() {
  local name="$1" port="$2"
  for _ in $(seq 1 30); do
    if port_up "$port"; then
      echo "  $name: up (:$port)"
      return 0
    fi
    sleep 3
  done
  echo "  $name: NOT up after 90s — check $RUN_DIR/$name.log"
  return 1
}
```

- `for _ in $(seq 1 30)` — we don't actually care what number we're on, just
  that we try up to 30 times, so the loop variable is named `_` as a
  convention meaning "I'm ignoring this."
- Inside, check if the port's up; if so, print success and `return 0`
  immediately (this exits the whole *function*, not just the loop — `return`
  jumps out of the function entirely).
- `sleep 3` — pause 3 seconds before trying again. 30 tries × 3 seconds = up
  to 90 seconds of patience before giving up, which is why the failure
  message says "after 90s."
- If we fall out of the loop without ever returning early, that means all 30
  tries failed — print the failure message and `return 1` (non-zero =
  failure, so callers can tell success from failure using `if wait_for ...;
  then`).

---

## 6. Reading command-line arguments, and defaults

When you run `./dev-up.sh down`, the word `down` becomes `$1` inside the
script (same numbering as function arguments — a script's own arguments
work exactly like a function's).

```bash
if [ "${1:-}" = "down" ]; then
  cmd_down
  exit 0
fi
```

`${1:-}` means "the value of `$1`, or an empty string if `$1` was never
given." Why not just `$1`? Because remember `set -u` from earlier — with
that safety switch on, referring to `$1` when no argument was passed at all
is treated as an error ("undefined variable"), and the whole script would
crash before it even got to check what you typed. `${var:-default}` is the
standard way to say "give me this variable's value, or fall back to
`default` if it's unset," which sidesteps that crash.

`exit 0` stops the script right there, successfully — everything below this
`if` block never runs when you pass `down`.

---

## 7. Running things in the background, and why `nohup` exists

Normally when a script runs a command, it waits for that command to finish
before moving to the next line. That's useless for a dev server — `ng serve`
never finishes on its own, it just runs forever until you stop it.

```bash
some-long-running-command &
```

The trailing `&` means "start this, but don't wait — immediately move on to
the next line." The command keeps running **in the background**.

Two problems with just `&` on its own, though:

1. If the terminal that launched it closes, the background command usually
   gets killed too (it receives a signal called `SIGHUP`, literally "hang
   up," a holdover from the days terminals were connected by phone lines).
2. Its output (anything it would normally print) still tries to print into
   your terminal, which gets messy once you've backgrounded several things.

`nohup` solves problem 1 — it explicitly tells the command "ignore hangup
signals," so it survives even after the terminal that started it is gone.
Redirecting its output solves problem 2:

```bash
nohup some-long-running-command > output.log 2>&1 &
```

- `> output.log` — send normal output (**stdout**) to a file instead of the
  screen.
- `2>&1` — send **stderr** (file descriptor 2) to wherever stdout (file
  descriptor 1) is currently going, which we just pointed at `output.log`.
  Read it right-to-left: "make descriptor 2 go where descriptor 1 goes."
  Every terminal program actually has two separate output channels — stdout
  for normal output, stderr for error/diagnostic messages — and by default
  both print to your screen, which is why you don't normally notice they're
  separate.
- The final `&` still backgrounds the whole thing.

This exact combination is why `dev-up.sh` has lines like:

```bash
( cd "$dir" && nohup bash -c "$cmd" > "$RUN_DIR/$name.log" 2>&1 & )
```

The `( ... )` wrapping it all is a **subshell** — everything inside those
parens runs in its own little sandboxed shell, so the `cd` inside doesn't
change the *main* script's current folder, only that temporary one's. That
matters because the very next line in the loop needs to `cd` into a
*different* project's folder to start the *next* service — without the
subshell, each `cd` would stack on the last one and you'd end up nested
somewhere nonsensical.

---

## 8. Reading output back out of a running background process: log files

Since we redirected each service's output into a file instead of the
screen, "check if it's working" means reading that file:

```bash
tail -20 /tmp/some.log     # last 20 lines
cat /tmp/some.log          # the whole file
```

`dev-up.sh` mentions the log path in every message it prints (`log:
$RUN_DIR/$name.log`) specifically so that if a service fails to start, you
know exactly which file to go read to find out why — this is a deliberate
debugging affordance, not an accident. When you write your own scripts that
background long-running things, always tell the human where the log went.

---

## 9. String surgery: pulling a value apart

`dev-up.sh` has this line for shutting things down by port:

```bash
for entry in "landlord-backend:8080" "barivara-backend:8081"; do
  name="${entry%%:*}"
  port="${entry##*:}"
  ...
done
```

`${entry%%:*}` and `${entry##*:}` look cryptic but follow one small rule:

- `%` means "trim from the **end**," `#` means "trim from the **start**."
- Doubling it (`%%` / `##`) means "match as much as possible" (greedy)
  instead of as little as possible.
- `:*` / `*:` is the pattern being trimmed — `*` is a wildcard meaning
  "anything."

So `${entry%%:*}` reads as "starting from `entry`, trim from the end
everything from the first `:` onward" → left with the part *before* the
colon. `${entry##*:}` reads as "trim from the start everything up through
the last `:`" → left with the part *after* the colon.

Given `entry="landlord-backend:8080"`, `name` becomes `landlord-backend` and
`port` becomes `8080`. This is bash's built-in way to split a string on a
separator without calling out to an external tool like `cut` or `awk` — fine
for a single simple split like this one.

---

## 10. Killing a process that owns a port

The genuinely tricky part of `dev-up.sh` was shutting things back down
reliably. The naive approach — remember the process ID (`$!`) of whatever
you started, and `kill` that PID later — sounds right but **doesn't
actually work** for `mvnw` (Maven) or `npx ng serve`, because both of those
are *wrapper* programs: they start, and then internally launch a *second*,
real process (the actual Java app, or the actual `ng serve` server) as a
child. Killing the wrapper doesn't always kill that child — depends on how
the wrapper is written, and both of these happen to leave orphans behind.
This is a genuinely common gotcha, not a beginner mistake — it wasted real
debugging time even while writing this project's script.

The fix that actually works: don't track *which process* you started at
all — instead, ask the operating system "whatever is currently listening on
this port, kill it," using a tool called `fuser`:

```bash
fuser -k -TERM 8080/tcp
```

This says "find whatever process has port 8080 open, and send it a `TERM`
signal (the polite way to ask a process to shut down)." It doesn't matter
whether that's a wrapper, a child, a grandchild — `fuser` finds the actual
process holding the port, because that's a fact the operating system
tracks directly, and kills exactly that one. This is a good general lesson:
when tracking "which process did I start" gets complicated, look for a way
to instead identify the thing you actually care about directly (here: "who
owns this port") rather than fighting the process tree.

---

## 11. Multi-line text blocks: heredocs

Printing several lines of formatted text with a bunch of separate `echo`
calls gets tedious. A **heredoc** lets you write a whole block as-is:

```bash
cat <<EOF
Line one
Line two, with a $variable in it
Line three
EOF
```

`cat <<EOF` means "feed `cat` everything up until a line that says just
`EOF`" (the word `EOF` is a convention, not a keyword — you could use any
word). Variables inside a heredoc *do* still get substituted (`$variable`
becomes its value) unless you quote the opening marker (`<<'EOF'`, with
quotes, turns that off and prints everything completely literally — useful
when the block itself contains `$` signs you want to keep as-is, like
example shell code).

`dev-up.sh` uses exactly this for its final summary message — much easier
to read and edit as one text block than as ten separate `echo` lines.

---

## 12. Putting it together — reading `dev-up.sh` end to end

Now that every piece has been introduced separately, read the real file
(`dev-up.sh` at the repo root) top to bottom. You should recognize every
single line:

1. **Shebang + safety switches** — `#!/usr/bin/env bash` and
   `set -uo pipefail` (§1, §3).
2. **`REPO` and `RUN_DIR`** — command substitution to find this script's own
   folder regardless of where it's called from, then `mkdir -p` to make sure
   a folder for logs exists (§2). `mkdir -p` specifically means "create this
   folder, and don't complain if it already exists" — the `-p` flag is what
   makes it safe to run every time instead of just the first time.
3. **`port_up`** — a function using `curl` + exit codes to answer "is
   something listening here?" (§4).
4. **`start_service`** — a function taking a name, port, folder, and command
   to run; skips if already running, otherwise backgrounds it with
   `nohup`/redirection inside a subshell (§7).
5. **`wait_for`** — a function looping up to 30 times with `sleep 3` between
   tries, reporting success or timeout (§5).
6. **`cmd_down`** — a function that walks a fixed list of `name:port` pairs,
   splitting each with the `%%`/`##` trick, and using `fuser -k` to actually
   kill whatever's on that port (§9, §10).
7. **The `if [ "${1:-}" = "down" ]` check** — decide "start everything" vs.
   "stop everything" based on the script's own first argument (§6).
8. **The actual work** — call `docker compose up -d` (start Postgres, doing
   nothing if it's already running — this is the same "idempotent" idea as
   `port_up` skipping already-running services), then call `start_service`
   four times, then `wait_for` four times, then print the final summary with
   a heredoc (§11).

Every concept in this script is one of the twelve things above, combined.
That's genuinely most of what you need for this whole class of script —
"start some local dev processes, check on them, be able to stop them
again." Bigger scripts add more tools (arrays, `case` statements, `getopts`
for fancier argument parsing, trapping signals) but the core shape doesn't
change.

---

## 13. A worked exercise — extend it yourself

To actually cement this, try adding a `status` mode to `dev-up.sh` yourself
before reading the answer below: `./dev-up.sh status` should print each
service's name and whether it's currently up or down, using `port_up`
(which already exists in the file) — no new concepts required, just
recombining what's already there.

<details>
<summary>One way to do it (there are several)</summary>

```bash
cmd_status() {
  for entry in "landlord-backend:8080" "barivara-backend:8081" "landlord-frontend:4200" "barivara-frontend:4201"; do
    name="${entry%%:*}"; port="${entry##*:}"
    if port_up "$port"; then
      echo "$name: up (:$port)"
    else
      echo "$name: down (:$port)"
    fi
  done
}

if [ "${1:-}" = "status" ]; then
  cmd_status
  exit 0
fi
```

Notice this is almost a copy-paste of `cmd_down`'s loop, just checking
instead of killing — that's normal. Once you notice two functions are 90%
identical, the next real skill (past what this lesson covers) is factoring
out the shared part; for a script this small, a little duplication is
completely fine and more readable than the abstraction would be.
</details>

---

## 14. Where to go from here

You now know enough to read and modify `dev-up.sh` confidently, and to write
your own small "start/check/stop some local processes" scripts from a blank
file. A few pointers for what's next, if you want to keep going:

- **`bash -x yourscript.sh`** runs a script printing every command as it
  executes, with variables already substituted — the single best debugging
  tool when a script isn't doing what you expect.
- **[shellcheck](https://www.shellcheck.net)** — paste a script in (or
  install it locally) and it flags common mistakes, including several this
  tutorial warned about (unquoted variables, etc.) automatically.
- **`man bash`** (or `help` inside bash for builtins like `if`/`for`) is the
  real reference once tutorials stop being enough — dense, but authoritative.
- **`case` statements** — once a script needs to handle more than two modes
  (`down` was our only "mode" here), `case "$1" in down) ...;; status)
  ...;; esac` reads better than a chain of `if`/`elif`.

None of that is required to understand this project's script — it's just
where the road keeps going.
