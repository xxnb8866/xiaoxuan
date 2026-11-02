
set -u
set -o pipefail

# =====================================================================
# [UI-0] 调色板 / 图标 / 画布
# =====================================================================
R=$'\033[0m'; BOLD=$'\033[1m'; DIM=$'\033[2m'; ITALIC=$'\033[3m'
HRED=$'\033[1;31m'; HGREEN=$'\033[1;32m'; HYELLOW=$'\033[1;33m'
HBLUE=$'\033[1;34m'; HMAGENTA=$'\033[1;35m'; HCYAN=$'\033[1;36m'; HWHITE=$'\033[1;37m'
P_ORANGE=$'\033[38;5;208m'; P_GOLD=$'\033[38;5;220m'; P_LIME=$'\033[38;5;118m'
P_MINT=$'\033[38;5;79m';   P_SKY=$'\033[38;5;81m';   P_VIOLET=$'\033[38;5;141m'
P_PINK=$'\033[38;5;213m';  P_GREY=$'\033[38;5;245m'; P_DEEP=$'\033[38;5;24m'
P_BLUE2=$'\033[38;5;33m'
BG_DARK=$'\033[48;5;236m'; BG_BLUE=$'\033[48;5;27m';  BG_GREEN=$'\033[48;5;34m'
BG_ORANGE=$'\033[48;5;208m'; BG_RED=$'\033[48;5;196m'; BG_VIOLET=$'\033[48;5;93m'

I_OK="✔"; I_ERR="✘"; I_WARN="⚠"; I_ARROW="➜"; I_STAR="★"; I_DOT="•"
I_PKG="▣"; I_GEAR="⚙"; I_ROCKET="➤"; I_FOLDER="▤"; I_SAVE="▾"; I_LOG="≡"
I_TIME="⏱"; I_FIRE="▲"; I_SPARK="✦"

# ---------------- 品牌 Logo: xxnb (ANSI Shadow, 逐行渐变) ----------------
LOGO_LINES=(
"██╗  ██╗██╗  ██╗███╗   ██╗██████╗ "
"╚██╗██╔╝╚██╗██╔╝████╗  ██║██╔══██╗"
" ╚███╔╝  ╚███╔╝ ██╔██╗ ██║██████╔╝"
" ██╔██╗  ██╔██╗ ██║╚██╗██║██╔══██╗"
"██╔╝ ██╗██╔╝ ██╗██║ ╚████║██████╔╝"
"╚═╝  ╚═╝╚═╝  ╚═╝╚═╝  ╚═══╝╚═════╝ "
)
LOGO_COLORS=("$P_SKY" "$P_MINT" "$P_LIME" "$P_GOLD" "$P_ORANGE" "$P_PINK")
LOGO_W=34

# ---------------- 吉祥物机器人（眼睛行可换字符做眨眼）----------------
# 宽 19，共 13 行；索引 5 = 眼睛行，6 = 嘴，11 = 胸口指示灯
# 结构：天线 → 头部外壳 → 护目镜面板(眼/嘴) → 脖子 → 机身(胸口灯) → 底座
ROBOT_LINES=(
"        ╭○╮        "
"         │         "
"   ╭─────┴─────╮   "
"  ╭┴───────────┴╮  "
"  │ ╭─────────╮ │  "
"  │ │  @   @  │ │  "   # @ 占位，运行时换成眼睛字符
"  │ │  ╰───╯  │ │  "   # 嘴（可换）
"  │ ╰─────────╯ │  "
"  ╰┬───────────┬╯  "
"   │           │   "
"  ╭┴───────────┴╮  "
"  │  ▪   ▪   ▪  │  "
"  ╰─────────────╯  "
)
ROBOT_COLORS=("$P_GOLD" "$P_SKY" "$P_SKY" "$P_SKY" \
              "$P_VIOLET" "$P_MINT" "$P_VIOLET" "$P_VIOLET" \
              "$P_SKY" "$P_SKY" "$P_VIOLET" "$P_VIOLET" "$P_VIOLET")
ROBOT_W=19
ROBOT_EYE_IDX=5
ROBOT_MOUTH_IDX=6
ROBOT_CHEST_IDX=11
ROBOT_MOUTH="╰───╯"

# 眼睛帧：睁大 → 半眯 → 眯 → 闭 → 眯 → 半眯（一轮约 1 秒）
EYE_FRAMES=("◉" "◕" "◔" "▔" "◔" "◕")
EYE_HAPPY="^"
EYE_SAD="×"

TERM_W=$(tput cols 2>/dev/null || echo 46)
BAR_W=22

# =====================================================================
# [UI-1] 日志环境：fd3 = 原始终端（进度条/动画），stdout = tee 落盘
# =====================================================================
export PREFIX="${PREFIX:-/data/data/com.termux/files/usr}"   # set -u 下防止未定义崩溃

WORK_DIR="$HOME/termux_build_workspace"
mkdir -p "$WORK_DIR" || { echo "无法创建工作区"; exit 1; }
mkdir -p "/storage/emulated/0/一键打包/日志" 2>/dev/null
LOG_FILE="/storage/emulated/0/一键打包/日志/build_$(date +"%Y%m%d_%H%M%S")_$$.log"
: > "$LOG_FILE"

exec 3>&1                                    # 终端直连（进度条/动画专用）
# stdout: 终端保留彩色 → 同时经 sed 去色后落盘，保证日志文件干净可读
exec > >(stdbuf -oL -eL tee >(stdbuf -oL sed 's/\x1b\[[0-9;]*[A-Za-z]//g' >> "$LOG_FILE")) 2>&1

trap 'ui_abort' INT TERM
trap 'exec 3>&- 2>/dev/null; sleep 0.2' EXIT

# =====================================================================
# [UI-2] 界面函数（全部用字符串拼接，规避 printf 参数错位）
# =====================================================================
TOTAL_STEPS=8
CUR_STEP=0
STEP_NAME="初始化"
START_TS=$(date +%s)

repeat_ch() { local n=$1 ch=$2 s="" i=0; for ((i=0;i<n;i++)); do s+="$ch"; done; printf "%s" "$s"; }

draw_bar() {  # draw_bar <当前> <总数> <阶段名> <颜色>
    local cur=$1 tot=$2 name=$3 c=$4
    local pct=$(( cur * 100 / tot ))
    local filled=$(( cur * BAR_W / tot ))
    local empty=$(( BAR_W - filled ))
    local out=""
    out+="${P_SKY}[STAGE ${HWHITE}${cur}/${tot}${P_SKY}]${R} "
    out+="${c}$(repeat_ch "$filled" '█')${R}${DIM}$(repeat_ch "$empty" '░')${R} "
    out+="${BOLD}$(printf "%3d" "$pct")%${R} "
    out+="${DIM}│${R} "
    out+="${c}${name}${R}"
    # 行尾迷你眼睛：随机眨一下，不碍事但一直有陪伴感
    local ee="◕"
    [ $((RANDOM % 4)) -eq 0 ] && ee="▔"
    out+=" ${P_PINK}${ee}${DIM}ᴗ${P_PINK}${ee}${R}"
    printf "  %s%s\n" "$out" "$R" >&3
}

step_begin() { CUR_STEP=$1; STEP_NAME="$2"; }
step_done()  {
    local el=$(( $(date +%s) - START_TS ))
    local el_txt="已用 $(( el / 60 ))分$(( el % 60 ))秒"
    local c; if [ "$CUR_STEP" -eq "$TOTAL_STEPS" ]; then c="$P_LIME"; else c="$P_ORANGE"; fi
    draw_bar "$CUR_STEP" "$TOTAL_STEPS" "$STEP_NAME" "$c"
    printf "  %s%s%s%s\n" "${DIM}${P_GREY}" "${I_TIME} " "${el_txt}" "${R}" >&3
}

ts() { date +"%H:%M:%S"; }

log() {  # log <级别> <文本> [缩进层数]
    local lvl="$1" txt="$2" ind="${3:-0}"
    local pad="" i=0; for ((i=0;i<ind;i++)); do pad+="  "; done
    local icon="" color=""
    case "$lvl" in
        ok)   icon="$I_OK";    color="$HGREEN" ;;
        err)  icon="$I_ERR";   color="$HRED" ;;
        warn) icon="$I_WARN";  color="$HYELLOW" ;;
        info) icon="$I_ARROW"; color="$HBLUE" ;;
        run)  icon="$I_GEAR";  color="$P_VIOLET" ;;
        star) icon="$I_STAR";  color="$P_GOLD" ;;
        pkg)  icon="$I_PKG";   color="$P_ORANGE" ;;
        *)    icon="$I_DOT";   color="$DIM" ;;
    esac
    printf "%s%s[%s]%s %s%s%s %s%s%s%s\n" \
        "${DIM}" "${P_GREY}" "$(ts)" "${R}" \
        "${color}" "${icon}" "${R}" \
        "${pad}" "${color}" "${txt}" "${R}"
}

rule() { local c="${1:-$P_DEEP}" w=$((TERM_W - 4)); [ "$w" -gt 58 ] && w=58; printf "  %s%s%s\n" "$c" "$(repeat_ch "$w" '─')" "$R"; }

stage_title() {  # stage_title <编号> <标题> <图标>
    printf "\n" >&3
    printf "  %s%s%s %s%s%s %s%s%s\n" "${BG_BLUE}" "${HWHITE}" " STEP $1 " "${R}" "${BOLD}${P_BLUE2}" "${3}" "${R}" "${HWHITE}" "${2}" "${R}" >&3
    rule "$P_DEEP" >&3
}

# ---- 机器人单行渲染（eye = 眼睛字符）----
robot_row() {
    local idx="$1"
    local eye="$2"
    local line="${ROBOT_LINES[$idx]}"
    local c="${ROBOT_COLORS[$idx]}"
    # 眼睛：先占位替换，再单独提亮成高亮青色，让眼神更突出
    if [ "$idx" -eq "$ROBOT_EYE_IDX" ]; then
        line="${line//@/$eye}"
        line="${line//$eye/${BOLD}${HCYAN}${eye}${c}}"
    fi
    # 嘴：可换成开心/难过两种弧度
    if [ "$idx" -eq "$ROBOT_MOUTH_IDX" ]; then
        line="${line//╰───╯/$ROBOT_MOUTH}"
    fi
    # 胸口指示灯：单独染成金色
    if [ "$idx" -eq "$ROBOT_CHEST_IDX" ]; then
        line="${line//▪/${P_GOLD}▪${c}}"
    fi
    printf "%s%s%s" "$c" "$line" "$R"
}

# ---- 结束卡片用的迷你表情（7 行，省地方）----
mini_face() {
    local e="$1" m="$2"
    printf "         %s%s%s\n" "${P_GOLD}" "○" "${R}" >&3
    printf "         %s%s%s\n" "${P_SKY}"  "│" "${R}" >&3
    printf "   %s%s%s\n"  "${P_SKY}"    "╭─────┴─────╮" "${R}" >&3
    printf "  %s%s%s\n"   "${P_SKY}"    "╭┴───────────┴╮" "${R}" >&3
    printf "  %s%s%s\n"   "${P_VIOLET}" "│   ${BOLD}${HCYAN}${e}${R}${P_VIOLET}     ${BOLD}${HCYAN}${e}${R}${P_VIOLET}   │" "${R}" >&3
    printf "  %s%s%s\n"   "${P_VIOLET}" "│    ${P_GOLD}${m}${P_VIOLET}    │" "${R}" >&3
    printf "  %s%s%s\n"   "${P_VIOLET}" "╰─────────────╯" "${R}" >&3
}

# ---- banner 第 i 行（并排 / 堆叠两种布局）----
banner_row() {
    local i=$1 eye="$2"
    local lrows=${#LOGO_LINES[@]} rrows=${#ROBOT_LINES[@]}
    if [ "$BANNER_SIDE" = "1" ]; then
        local left="" right=""
        if [ "$i" -lt "$lrows" ]; then left="${LOGO_COLORS[$i]}${LOGO_LINES[$i]}${R}"
        else left="$(repeat_ch "$LOGO_W" ' ')"; fi
        if [ "$i" -lt "$rrows" ]; then right=$(robot_row "$i" "$eye"); fi
        printf "  %s   %s\n" "$left" "$right"
    else
        if [ "$i" -lt "$lrows" ]; then
            printf "  %s%s%s\n" "${LOGO_COLORS[$i]}" "${LOGO_LINES[$i]}" "$R"
        else
            printf "  %s\n" "$(robot_row "$((i - lrows))" "$eye")"
        fi
    fi
}

draw_banner() {
    local lrows=${#LOGO_LINES[@]} rrows=${#ROBOT_LINES[@]}
    BANNER_SIDE=0
    [ "$TERM_W" -ge "$((LOGO_W + ROBOT_W + 5))" ] && BANNER_SIDE=1
    if [ "$BANNER_SIDE" = "1" ]; then
        BANNER_N=$lrows; [ "$rrows" -gt "$BANNER_N" ] && BANNER_N=$rrows
        BANNER_EYE_ROW=$((ROBOT_EYE_IDX + 1))
    else
        BANNER_N=$((lrows + rrows))
        BANNER_EYE_ROW=$((lrows + ROBOT_EYE_IDX + 1))
    fi
    local i
    for ((i=0;i<BANNER_N;i++)); do banner_row "$i" "◕" >&3; done
}

# ---- 眨眼动画：只重绘眼睛所在行，其余内容不动 ----
blink_eyes() {
    local rounds="${1:-2}" r f
    local up=$(( BANNER_N - BANNER_EYE_ROW + 1 ))
    local down=$(( BANNER_N - BANNER_EYE_ROW ))
    [ "$up" -lt 1 ] && return 0
    for ((r=0;r<rounds;r++)); do
        for f in "${EYE_FRAMES[@]}"; do
            printf "\033[%dA" "$up" >&3
            banner_row "$((BANNER_EYE_ROW - 1))" "$f" >&3
            printf "\033[%dB" "$down" >&3
            sleep 0.16
        done
    done
    printf "\033[%dA" "$up" >&3
    banner_row "$((BANNER_EYE_ROW - 1))" "◕" >&3
    printf "\033[%dB" "$down" >&3
}

banner() {
    draw_banner
    printf "\n" >&3
    printf "  %s%s%s %s%s v5 · 通用自动打包%s\n" "${BG_VIOLET}" "${HWHITE}" " TERMUX " "${R}" "${BOLD}${P_VIOLET}" "${R}" >&3
    printf "  %s%s 全类型识别 · 参数自动推导 · 实时日志流 %s\n" "${DIM}${P_GREY}" "${ITALIC}" "${R}" >&3
    printf "\n" >&3
}

# 实时日志流：逐行转发 + 竖线前缀（while read 天然行缓冲）
run_stream() {
    local desc="$1"; shift
    local pad="" i=0; for ((i=0;i<1;i++)); do pad+="  "; done
    log run "$desc"
    local rc=0
    if command -v stdbuf >/dev/null 2>&1; then
        stdbuf -oL -eL "$@" 2>&1 | while IFS= read -r line; do
            printf "%s%s│%s %s%s%s%s\n" "${DIM}" "${P_DEEP}" "${R}" "${pad}" "${DIM}" "${line}" "${R}"
        done
        rc=${PIPESTATUS[0]}
    else
        "$@" 2>&1 | while IFS= read -r line; do
            printf "%s%s│%s %s%s%s%s\n" "${DIM}" "${P_DEEP}" "${R}" "${pad}" "${DIM}" "${line}" "${R}"
        done
        rc=${PIPESTATUS[0]}
    fi
    return $rc
}

spinner_pid=""
spinner_start() {
    local msg="$1"
    ( local f=("⠋" "⠙" "⠹" "⠸" "⠼" "⠴" "⠦" "⠧" "⠇" "⠏") i=0
      while :; do
          printf "\r  %s%s%s %s%s%s\033[K" "${P_ORANGE}" "${f[i]}" "${R}" "${DIM}" "${msg}" "${R}" >&3
          i=$(( (i+1) % 10 )); sleep 0.12
      done ) &
    spinner_pid=$!
}
spinner_stop() {
    if [ -n "$spinner_pid" ]; then kill "$spinner_pid" 2>/dev/null; wait "$spinner_pid" 2>/dev/null; fi
    spinner_pid=""
    printf "\r\033[K" >&3
}

ui_abort() {
    spinner_stop
    printf "\n" >&3
    printf "  %s%s%s %s%s 已中断：清理子进程与临时文件…%s\n" "${BG_RED}" "${HWHITE}" " ABORT " "${R}" "${HRED}${I_ERR}" "${R}" >&3
    log err "用户中断打包流程"
    pkill -P $$ 2>/dev/null
    sleep 1
    exit 1
}

# =====================================================================
# [用户档案] 记住名字与打包次数，存在 ~/.termux_builder.conf
# =====================================================================
CONF_FILE="$HOME/.termux_builder.conf"
USER_NAME=""; BUILD_COUNT=0

load_conf() {
    [ -f "$CONF_FILE" ] || return 0
    USER_NAME=$(sed -n 's/^USER_NAME=//p'   "$CONF_FILE" 2>/dev/null | head -1)
    local c; c=$(sed -n 's/^BUILD_COUNT=//p' "$CONF_FILE" 2>/dev/null | head -1)
    [[ "$c" =~ ^[0-9]+$ ]] && BUILD_COUNT=$((10#$c)) || BUILD_COUNT=0
}
save_conf() {
    { echo "USER_NAME=$USER_NAME"; echo "BUILD_COUNT=$BUILD_COUNT"; } > "$CONF_FILE" 2>/dev/null
}
greet_word() {
    local h=$((10#$(date +%H)))
    if   [ "$h" -lt 5  ]; then echo "夜深了"
    elif [ "$h" -lt 11 ]; then echo "早上好"
    elif [ "$h" -lt 14 ]; then echo "中午好"
    elif [ "$h" -lt 18 ]; then echo "下午好"
    else                       echo "晚上好"; fi
}
load_conf

# ---------------------------------------------------------------- 启动
clear 2>/dev/null
draw_banner
blink_eyes 2
printf "\n" >&3
printf "  %s%s%s %s%s v5 · 通用自动打包%s\n" "${BG_VIOLET}" "${HWHITE}" " TERMUX " "${R}" "${BOLD}${P_VIOLET}" "${R}" >&3
printf "  %s%s 全类型识别 · 参数自动推导 · 实时日志流 %s\n" "${DIM}${P_GREY}" "${ITALIC}" "${R}" >&3
printf "\n" >&3

# 首次见面问名字，之后直接认人
if [ -z "$USER_NAME" ]; then
    printf "  %s%s%s %s%s 初次见面，怎么称呼你？%s " \
        "${BG_VIOLET}" "${HWHITE}" " HELLO " "${R}" "${BOLD}${P_PINK}" "${R}" >&3
    while :; do
        read -r -e USER_NAME
        USER_NAME="${USER_NAME#"${USER_NAME%%[![:space:]]*}"}"   # 去首尾空格
        USER_NAME="${USER_NAME%"${USER_NAME##*[![:space:]]}"}"
        if [ -z "$USER_NAME" ]; then USER_NAME="指挥官"; break; fi
        # 误把路径当名字的情况直接挡掉
        if [[ "$USER_NAME" == *"/"* ]] || [ "${#USER_NAME}" -gt 16 ]; then
            printf "  %s%s 这不像个名字，换个短一点的？%s " "${HYELLOW}${I_WARN}${R}" "${DIM}" "${R}" >&3
            continue
        fi
        break
    done
    save_conf
    printf "  %s%s 记住啦，以后就叫你 %s%s%s %s\n" \
        "${P_MINT}${I_OK}${R}" "${DIM}" "${BOLD}${P_GOLD}" "$USER_NAME" "${R}" "${DIM}(改名请编辑 $CONF_FILE)${R}" >&3
else
    printf "  %s%s%s %s%s%s%s %s%s，%s%s\n" \
        "${BG_VIOLET}" "${HWHITE}" " HELLO " "${R}" \
        "${BOLD}${P_GOLD}" "$USER_NAME" "${R}" \
        "${P_MINT}" "$(greet_word)" "${DIM}这是你第 ${BUILD_COUNT} 次打包${R}" >&3
fi
printf "\n" >&3

printf "  %s%s 日志 %s %s%s%s\n" "${DIM}${I_LOG}" "${R}" "${DIM}" "${R}" "${P_GREY}" "$LOG_FILE" >&3
printf "  %s%s 设备 %s %s%s%s %s(%s)%s\n" "${DIM}${I_GEAR}" "${R}" "${DIM}" "${R}" "${P_GREY}" \
    "$(uname -o 2>/dev/null || echo Android)" "${DIM}" "$(uname -m)" "${R}" >&3
printf "\n" >&3
draw_bar 0 "$TOTAL_STEPS" "启动中" "$P_ORANGE"

# =====================================================================
# [工具函数区] —— 与 v3 逻辑完全一致
# =====================================================================
install_pkg_if_missing() {
    local pkg_name="$1" bin_name="$2"
    if ! command -v "$bin_name" &>/dev/null; then
        log pkg "安装缺失依赖 ${pkg_name}" 1
        pkg install -y "$pkg_name" >/dev/null 2>&1 || { log err "安装 ${pkg_name} 失败"; exit 1; }
    fi
}

find_shallowest() {
    local root="$1"; shift
    local args=() n first=1
    for n in "$@"; do
        if [ $first -eq 1 ]; then args=(-name "$n"); first=0
        else args+=(-o -name "$n"); fi
    done
    [ $first -eq 1 ] && return 0
    find "$root" -maxdepth 10 -type f \( "${args[@]}" \) -printf '%d\t%p\n' 2>/dev/null \
        | sort -n -k1,1 -k2,2 | head -1 | cut -f2-
}

strip_crlf() {
    local f
    while IFS= read -r -d '' f; do
        sed -i 's/\r$//' "$f" 2>/dev/null
    done < <(find "$1" -type f \( -name "*.sh" -o -name "gradlew" -o -name "*.gradle" \
             -o -name "*.kts" -o -name "*.properties" -o -name "*.xml" -o -name "*.mk" \
             -o -name "CMakeLists.txt" \) -print0 2>/dev/null)
}

calc_xmx() {
    local total_mb
    total_mb=$(free -m 2>/dev/null | awk '/Mem:/{print $2}')
    [ -z "${total_mb:-}" ] && total_mb=4096
    local x=$(( total_mb / 4 ))
    [ "$x" -gt 1536 ] && x=1536
    [ "$x" -lt 512 ] && x=512
    echo "${x}m"
}

detect_compile_sdk() {
    local v=""
    v=$(grep -rhoE 'compileSdk(Version)?[[:space:]]*[= ][[:space:]]*[0-9]{2}' \
        --include="*.gradle" --include="*.gradle.kts" . 2>/dev/null \
        | grep -oE '[0-9]{2}$' | sort -n | tail -1)
    # Kotlin DSL 兜底：compileSdk("35") 或 compileSdk(35)
    if [ -z "$v" ]; then
        v=$(grep -rhoE 'compileSdk[[:space:]]*[(=][[:space:]]*[0-9]+' \
            --include="*.gradle" --include="*.gradle.kts" . 2>/dev/null \
            | grep -oE '[0-9]+' | sort -n | tail -1)
    fi
    if [ -z "$v" ]; then
        v=$(grep -rhoE 'targetSdk(Version)?[[:space:]]*[= ][[:space:]]*[0-9]{2}' \
            --include="*.gradle" --include="*.gradle.kts" . 2>/dev/null \
            | grep -oE '[0-9]{2}$' | sort -n | tail -1)
    fi
    if [ -z "$v" ] && [ -f "AndroidManifest.xml" ]; then
        v=$(grep -oE 'targetSdkVersion="[0-9]+"' AndroidManifest.xml 2>/dev/null | grep -oE '[0-9]+' | head -1)
    fi
    if [ -z "$v" ] && [ -f "project.properties" ]; then
        v=$(sed -n 's/^target=android-\([0-9]*\)/\1/p' project.properties 2>/dev/null | head -1)
    fi
    echo "${v:-35}"
}

detect_gradle_version() {
    local v=""
    if [ -f "gradle/wrapper/gradle-wrapper.properties" ]; then
        v=$(grep -v '^[[:space:]]*#' gradle/wrapper/gradle-wrapper.properties 2>/dev/null | sed -n 's|.*gradle-\([0-9]*\)\.[0-9.]*-.*|\1|p' \
            gradle/wrapper/gradle-wrapper.properties 2>/dev/null | head -1)
    fi
    echo "$v"
}

detect_aapt2() {
    local c
    for c in /data/data/com.termux/files/usr/bin/aapt2 "$(command -v aapt2 2>/dev/null)"; do
        if [ -n "$c" ] && [ -x "$c" ]; then echo "$c"; return 0; fi
    done
    echo ""
}

detect_build_tools_ver() {
    local d
    d=$(ls -d "$ANDROID_HOME"/build-tools/*/ 2>/dev/null | sort -V | tail -1)
    [ -n "$d" ] && basename "$d" || echo "35.0.0"
}

refresh_java_home() {
    local jb=""
    jb=$(command -v java 2>/dev/null)
    [ -z "$jb" ] && [ -f "$PREFIX/lib/jvm/java-21-openjdk/bin/java" ] && jb="$PREFIX/lib/jvm/java-21-openjdk/bin/java"
    [ -z "$jb" ] && [ -f "$PREFIX/lib/jvm/java-17-openjdk/bin/java" ] && jb="$PREFIX/lib/jvm/java-17-openjdk/bin/java"
    if [ -n "$jb" ]; then
        export JAVA_HOME=$(dirname "$(dirname "$(realpath "$jb")")")
        log ok "JAVA_HOME = ${JAVA_HOME}" 1
    else
        log warn "解析不到 JAVA_HOME" 1
    fi
}

# ---- 产物后处理：对齐 + 补签 ----
# 安全原则：已签名的包一律不碰（zipalign 会破坏既有签名），只处理未签名的
post_process_apk() {
    local apk="$1"
    [ "${apk##*.}" = "apk" ] || return 0
    command -v apksigner >/dev/null 2>&1 || return 0

    if apksigner verify --print-certs "$apk" >/dev/null 2>&1; then
        log ok "APK 签名有效，无需处理" 1
        return 0
    fi

    log warn "检测到未签名 APK，执行对齐 + 补签" 1
    if command -v zipalign >/dev/null 2>&1; then
        local aligned="${apk%.apk}-aligned.apk"
        if zipalign -f -p 4 "$apk" "$aligned" >/dev/null 2>&1 && [ -s "$aligned" ]; then
            cp -f "$apk" "${apk}.original" 2>/dev/null
            mv -f "$aligned" "$apk"; log ok "zipalign 4 字节对齐完成" 1
        else
            rm -f "$aligned" 2>/dev/null; log warn "zipalign 失败，跳过对齐" 1
        fi
    fi

    local ks="$HOME/.android/debug.keystore"
    if [ ! -f "$ks" ] && command -v keytool >/dev/null 2>&1; then
        mkdir -p "$HOME/.android"
        log info "生成 debug keystore…" 1
        keytool -genkeypair -v -keystore "$ks" -alias androiddebugkey \
            -keyalg RSA -keysize 2048 -validity 10000 \
            -storepass android -keypass android \
            -dname "CN=Android Debug,O=Android,C=US" >/dev/null 2>&1
    fi
    if [ -f "$ks" ]; then
        if apksigner sign --ks "$ks" --ks-key-alias androiddebugkey \
                --ks-pass pass:android --key-pass pass:android \
                --v1-signing-enabled true --v2-signing-enabled true "$apk" >/dev/null 2>&1; then
            log ok "补签完成（可直接安装）" 1
        else
            log warn "补签失败，需手动签名后安装" 1
        fi
    else
        log warn "无可用 keystore，跳过补签" 1
    fi
}

# ---- CMake 版本探测（externalNativeBuild { cmake { version "x.y.z" } }）----
detect_cmake_version() {
    local v=""
    v=$(grep -rhoE 'cmake[[:space:]]*\{[^{}]*version[[:space:]]*["\047]?[0-9]+\.[0-9]+\.[0-9]+' \
        --include="*.gradle" --include="*.gradle.kts" . 2>/dev/null \
        | grep -oE '[0-9]+\.[0-9]+\.[0-9]+' | head -1)
    [ -z "$v" ] && v=$(grep -rhoE 'cmakeVersion[[:space:]]*[= ][[:space:]]*["\047][0-9.]+' \
        --include="*.gradle" --include="*.gradle.kts" . 2>/dev/null \
        | grep -oE '[0-9]+\.[0-9]+\.[0-9]+' | head -1)
    echo "${v:-3.22.1}"
}

# ---- gradlew 是否"真的能跑"（有 wrapper jar 才算，否则 ./gradlew 会直接失败）----
gradlew_usable() {
    [ -f "gradlew" ] || return 1
    [ -f "gradle/wrapper/gradle-wrapper.jar" ] || return 1
    return 0
}

# ---- Gradle DSL 自愈：构建脚本里某方法与当前 AGP 不匹配时，自动注释掉并重试 ----
# 典型场景：AGP 7.0 起 noCompress 从 packagingOptions 搬走，
# 报 "Could not find method noCompress() for arguments [js] on ...PackagingOptions"
# 这类配置几乎都是可选优化项，注释掉不影响功能，比整轮失败划算得多。
heal_gradle_dsl() {
    local lg="${LOG_FILE:-/dev/null}"
    [ -f "$lg" ] || return 1
    local m
    m=$(sed 's/\x1b\[[0-9;]*[A-Za-z]//g' "$lg" 2>/dev/null \
        | grep -oiE "Could not find method [a-zA-Z0-9_]+\\(\\)" \
        | head -1 | sed -E 's/.*method ([a-zA-Z0-9_]+)\(\)/\1/')
    [ -n "$m" ] || return 1

    local gf found=0
    for gf in $(find . -maxdepth 3 \( -name "build.gradle" -o -name "build.gradle.kts" \) 2>/dev/null); do
        if grep -qE "^[[:space:]]*${m}[[:space:]]*[\(']" "$gf" 2>/dev/null; then
            sed -E -i "s|^([[:space:]]*)(${m}[[:space:]]*[\('].*)$|\1// [auto-removed] \2|" "$gf" 2>/dev/null
            log warn "自动注释不兼容配置 ${m}() ← $(basename "$gf")" 1
            found=1
        fi
    done
    [ "$found" -eq 1 ] || return 1
    return 0
}

# ---- 构建失败时，从日志里抓典型错误并给出可执行的修复建议 ----
diagnose_build_failure() {
    local lg="${LOG_FILE:-/dev/null}"
    [ -f "$lg" ] || return 0
    local tail_txt
    tail_txt=$(sed 's/\x1b\[[0-9;]*[A-Za-z]//g' "$lg" 2>/dev/null | tail -n 120)

    printf "\n" >&3
    printf "  %s%s%s %s%s 失败原因速查%s\n" "${BG_RED}" "${HWHITE}" " WHY " "${R}" "${HYELLOW}${I_WARN}" "${R}" >&3

    local found=0
    if printf '%s' "$tail_txt" | grep -qiE "was not found in SDK|CMake '.*' was not found|No CMake version found"; then
        printf "  %s CMake 未安装%s → sdkmanager --install 'cmake;%s'\n" \
            "${P_ORANGE}${I_ARROW}${R}" "${R}" "$(detect_cmake_version 2>/dev/null)" >&3
        found=1
    fi
    if printf '%s' "$tail_txt" | grep -qiE "gradle-wrapper\.jar|Could not find or load main class org\.gradle\.wrapper"; then
        printf "  %s gradle-wrapper.jar 缺失%s → 已自动改用系统 gradle；若仍失败请 pkg install gradle\n" \
            "${P_ORANGE}${I_ARROW}${R}" "${R}" >&3
        found=1
    fi
    if printf '%s' "$tail_txt" | grep -qiE "arpa/inet\.h file not found|sysroot|not found in '<sysroot>'"; then
        printf "  %s 原生编译被 NDK clang 劫持%s → 已自动把 NDK bin 移出 PATH 并强制 CXX=%s\n" \
            "${P_ORANGE}${I_ARROW}${R}" "${R}" "${CXX:-$PREFIX/bin/clang++}" >&3
        found=1
    fi
    # AGP 7.0 起不少 DSL 搬了家（如 noCompress → androidResources），
    # 表现为 "Could not find method xxx()"，本质是构建脚本与 AGP 版本不匹配
    if printf '%s' "$tail_txt" | grep -qiE "Could not find method [a-zA-Z]+\(\)"; then
        BADM=$(printf '%s' "$tail_txt" | grep -oiE "Could not find method [a-zA-Z]+\(\)" \
               | head -1 | grep -oE "[a-zA-Z]+\(\)" | tr -d '()')
        printf "  %s 构建脚本 DSL 与 AGP 版本不匹配（%s）%s\n" \
            "${P_ORANGE}${I_ARROW}${R}" "${BADM:-未知方法}" "${R}" >&3
        printf "    %s AGP 7.0+ 常见搬家：noCompress → androidResources{ }%s\n" "${P_GREY}" "${R}" >&3
        printf "    %s 最快解法：删掉该配置块（多数只是优化项，不影响功能）%s\n" "${P_GREY}" "${R}" >&3
        found=1
    fi
    if printf '%s' "$tail_txt" | grep -qiE "installed build tools revision|is corrupted|build-tools"; then
        printf "  %s build-tools 版本对不上%s → 本脚本已自动对齐；或 sdkmanager --install 'build-tools;33.0.2'\n" \
            "${P_ORANGE}${I_ARROW}${R}" "${R}" >&3
        found=1
    fi
    if printf '%s' "$tail_txt" | grep -qiE "Could not resolve|Could not download|Could not find com\.android\.tools\.build|plugin.*not found|Network is unreachable|UnknownHostException"; then
        printf "  %s 依赖下载失败（多为断网/仓库不可达）%s → 联网后重试，或改用国内镜像\n" \
            "${P_ORANGE}${I_ARROW}${R}" "${R}" >&3
        found=1
    fi
    if printf '%s' "$tail_txt" | grep -qiE "installed build tools|build-tools.*not found|failed to find Build Tools"; then
        printf "  %s build-tools 缺失%s → sdkmanager --install 'build-tools;33.0.2'\n" \
            "${P_ORANGE}${I_ARROW}${R}" "${R}" >&3
        found=1
    fi
    if printf '%s' "$tail_txt" | grep -qiE "SDK location not found|sdk\.dir|ANDROID_HOME"; then
        printf "  %s SDK 路径问题%s → 本脚本已写 local.properties；或 export ANDROID_HOME=%s\n" \
            "${P_ORANGE}${I_ARROW}${R}" "${R}" "${ANDROID_HOME:-$HOME/android-sdk}" >&3
        found=1
    fi
    if printf '%s' "$tail_txt" | grep -qiE "Unsupported class file major|Minimum supported Gradle|requires Java|java\.lang\.UnsupportedClassVersionError"; then
        printf "  %s JDK 版本不匹配%s → AGP 7.x 用 JDK 11/17：pkg install openjdk-17\n" \
            "${P_ORANGE}${I_ARROW}${R}" "${R}" >&3
        found=1
    fi
    if [ "$found" -eq 0 ]; then
        printf "  %s 未匹配到已知错误模式，请看日志最后 30 行：%s\n" "${P_GREY}${I_DOT}${R}" "${R}" >&3
        printf '%s\n' "$tail_txt" | tail -n 30 | while IFS= read -r ln; do
            printf "    %s%s%s\n" "${DIM}${P_GREY}" "$ln" "${R}" >&3
        done
    fi
    printf "\n" >&3
}

# ---- 确保 build-tools 可用，并把 build.gradle 里的版本对齐到实际安装版本 ----
ensure_build_tools() {
    local bt have=""
    # 优先顺序：AGP 7.4 默认要 33.0.x，故 33.0.2 排最前
    for bt in "33.0.2" "$(detect_build_tools_ver)" "34.0.0" "33.0.1" "30.0.3"; do
        [ -n "$bt" ] || continue
        if [ -d "$ANDROID_HOME/build-tools/$bt" ]; then have="$bt"; break; fi
    done

    if [ -z "$have" ] && command -v sdkmanager &>/dev/null; then
        for bt in "33.0.2" "34.0.0" "33.0.1" "30.0.3"; do
            log info "尝试安装 build-tools;$bt" 1
            yes | sdkmanager --licenses >/dev/null 2>&1
            sdkmanager "build-tools;$bt" >/dev/null 2>&1
            if [ -d "$ANDROID_HOME/build-tools/$bt" ]; then have="$bt"; break; fi
        done
    fi

    if [ -z "$have" ]; then
        have=$(ls -1 "$ANDROID_HOME/build-tools" 2>/dev/null | sort -V | tail -1)
    fi

    if [ -n "$have" ]; then
        log ok "build-tools ${have} 可用" 1
        export PATH="$ANDROID_HOME/build-tools/$have:$PATH"
        # 把项目里写死的版本改成实际可用版本，避免 "revision xx is corrupted"
        local gf
        for gf in $(find . -maxdepth 3 \( -name "build.gradle" -o -name "build.gradle.kts" \) 2>/dev/null); do
            grep -qE "buildToolsVersion" "$gf" 2>/dev/null || continue
            sed -i "s/buildToolsVersion[[:space:]]*['\"][0-9][0-9.]*['\"]/buildToolsVersion '$have'/g" "$gf" 2>/dev/null
        done
    else
        log warn "SDK 无可用 build-tools，编译会失败；手动：sdkmanager --install 'build-tools;33.0.2'" 1
    fi
}

# ---- 为"原生编译"准备干净工具链：强制用 Termux 自己的编译器 ----
setup_host_compiler() {
    export PATH="$PREFIX/bin:$PATH"
    local cc=""
    for c in "$PREFIX/bin/clang++" "$PREFIX/bin/g++" "$PREFIX/bin/c++"; do
        if [ -x "$c" ]; then cc="$c"; break; fi
    done
    if [ -z "$cc" ] && command -v clang++ &>/dev/null; then cc="$(command -v clang++)"; fi
    if [ -n "$cc" ]; then
        export CXX="$cc"
        case "$cc" in
            *clang++) export CC="${cc%++}" ;;
            *g++)     export CC="${cc%++}cc" ;;
            *c++)     export CC="${cc%++}c"  ;;
            *)        export CC="cc" ;;
        esac
        [ -x "$CC" ] || export CC="cc"
    fi
}

# ---- 读取 APK 元信息（用于结束卡片展示）----
APK_PKG=""; APK_VNAME=""; APK_VCODE=""; APK_MINSDK=""; APK_TGTSDK=""
read_apk_info() {
    local apk="$1"
    command -v aapt >/dev/null 2>&1 || return 0
    [ "${apk##*.}" = "apk" ] || return 0
    local dump pline
    dump=$(aapt dump badging "$apk" 2>/dev/null)
    [ -z "$dump" ] && return 0
    pline=$(printf '%s\n' "$dump" | grep -m1 '^package:')
    [ -z "$pline" ] && return 0
    APK_PKG=$(printf '%s' "$pline" | grep -oE "^package: name='[^']*'" | head -1 | cut -d"'" -f2)
    APK_VNAME=$(printf '%s' "$pline" | grep -oE "versionName='[^']*'" | head -1 | cut -d"'" -f2)
    APK_VCODE=$(printf '%s' "$pline" | grep -oE "versionCode='[^']*'" | head -1 | cut -d"'" -f2)
    # 真实 aapt 把 sdkVersion / targetSdkVersion 输出在 package 行之后的独立行
    APK_MINSDK=$(printf '%s\n' "$dump" | grep -oE "^sdkVersion:'[^']*'"       | head -1 | cut -d"'" -f2)
    APK_TGTSDK=$(printf '%s\n' "$dump" | grep -oE "^targetSdkVersion:'[^']*'" | head -1 | cut -d"'" -f2)
}

# =====================================================================
# [STEP 1/8] 环境自检
# =====================================================================
step_begin 1 "环境自检"
stage_title "1/8" "环境自检与预修复" "$I_GEAR"

if [ ! -d "$HOME/storage" ]; then
    log warn "未检测到存储权限，正在申请…"
    termux-setup-storage && sleep 2
    log ok "存储权限已申请"
else
    log ok "存储权限正常"
fi

export ANDROID_HOME="$HOME/android-sdk"
export ANDROID_SDK_ROOT="$ANDROID_HOME"

# ---- NDK 目录自动探测（兼容 ~/android-ndk、~/android-ndk-r26d、~/android-sdk/ndk/* 等）----
detect_ndk_home() {
    local c best="" hv=0 v n
    for c in "$HOME/android-ndk" "$HOME"/android-ndk-r* "$ANDROID_HOME/ndk-bundle"              "$ANDROID_HOME"/ndk/* "$HOME"/ndk*; do
        [ -d "$c" ] || continue
        [ -d "$c/toolchains" ] || [ -f "$c/ndk-build" ] || continue
        # 优先取版本目录（android-sdk/ndk/26.1.10909125 这类）
        n=$(basename "$c")
        v=$(printf '%s' "$n" | grep -oE '^[0-9]+' | head -1)
        if [ -n "$v" ] && [ "$v" -ge "$hv" ] 2>/dev/null; then hv="$v"; best="$c"; fi
        [ -z "$best" ] && best="$c"
    done
    echo "$best"
}
export ANDROID_NDK_HOME="$(detect_ndk_home)"
[ -z "$ANDROID_NDK_HOME" ] && export ANDROID_NDK_HOME="$HOME/android-ndk"
export ANDROID_NDK_ROOT="$ANDROID_NDK_HOME"
export NDK_HOME="$ANDROID_NDK_HOME"

case "$(uname -m)" in
    aarch64)           NDK_HOST_TAG="linux-aarch64" ;;
    armv7l|armv8l|arm) NDK_HOST_TAG="linux-arm" ;;
    x86_64|amd64)      NDK_HOST_TAG="linux-x86_64" ;;
    i686|i386)         NDK_HOST_TAG="linux-x86" ;;
    *)                 NDK_HOST_TAG="linux-aarch64" ;;
esac
NDK_TC="$ANDROID_NDK_HOME/toolchains/llvm/prebuilt/$NDK_HOST_TAG/bin"

# ★ 关键修复：不要把 NDK 的 bin 目录塞进 PATH！
#   里面也有 clang/clang++，前置后会劫持 Termux 原生编译（build.sh 里的 C++ 工程），
#   而 NDK clang 裸跑缺 --sysroot，必然报：
#       fatal error: 'arpa/inet.h' file not found
#   Gradle/CMake/ndk-build 都会自己按 ndk.dir 找编译器，根本不需要它进 PATH。
#   只把 SDK 的命令行工具加进去（这些不含编译器，安全）。
export PATH="$ANDROID_HOME/cmdline-tools/latest/bin:$ANDROID_HOME/platform-tools:$PATH"
if [ -d "$ANDROID_HOME/build-tools/$(detect_build_tools_ver)" ]; then
    export PATH="$ANDROID_HOME/build-tools/$(detect_build_tools_ver):$PATH"
fi
# 确保 Termux 自己的编译器优先级最高，杜绝 NDK clang 劫持
export PATH="$PREFIX/bin:$PATH"

log ok "SDK  ${ANDROID_HOME}"
log ok "NDK  ${ANDROID_NDK_HOME}"
log ok "架构 ${NDK_HOST_TAG}"

log info "检查基础依赖…"
for p in "unzip:unzip" "zip:zip" "tar:tar" "p7zip:7z" "git:git" "jq:jq" \
         "which:which" "cmake:cmake" "make:make" "ant:ant" "maven:mvn" \
         "aapt:aapt" "aapt2:aapt2" "apksigner:apksigner" "d8:d8" \
         "zipalign:zipalign" \
         "coreutils:realpath" "sed:sed" "gawk:awk" "grep:grep" "findutils:find"; do
    install_pkg_if_missing "${p%%:*}" "${p##*:}"
done
log ok "基础依赖检查完毕"
step_done

# =====================================================================
# [STEP 2/8] 获取输入
# =====================================================================
step_begin 2 "获取输入"
stage_title "2/8" "获取项目路径" "$I_FOLDER"
printf "  %s%s%s %s%s请输入压缩包或项目文件夹路径:%s " "${BOLD}${P_MINT}" "▸" "${R}" "${BOLD}" "${HWHITE}" "${R}" >&3
read -r -e INPUT_PATH
if [ -z "${INPUT_PATH:-}" ]; then log err "路径为空"; exit 1; fi
INPUT_PATH="${INPUT_PATH/#\~/$HOME}"
INPUT_PATH=$(echo "$INPUT_PATH" | tr -d '"' | tr -d "'")
if [ ! -e "$INPUT_PATH" ]; then log err "找不到路径: $INPUT_PATH"; exit 1; fi
log ok "输入路径有效 ${INPUT_PATH}"
step_done

# =====================================================================
# [STEP 3/8] 解压
# =====================================================================
step_begin 3 "解压工作区"
stage_title "3/8" "解压与清洗" "$I_PKG"
if [ -f "$INPUT_PATH" ]; then
    rm -rf "$WORK_DIR"; mkdir -p "$WORK_DIR"
    rc=0
    case "$INPUT_PATH" in
        *.zip|*.ZIP)            spinner_start "解压 ZIP…";            unzip -q -o "$INPUT_PATH" -d "$WORK_DIR"; rc=$?; spinner_stop ;;
        *.tar.gz|*.tgz|*.tar.Z) spinner_start "解压 TAR.GZ…";         tar -xzf "$INPUT_PATH" -C "$WORK_DIR";   rc=$?; spinner_stop ;;
        *.tar.xz|*.txz)         spinner_start "解压 TAR.XZ…";         tar -xJf "$INPUT_PATH" -C "$WORK_DIR";   rc=$?; spinner_stop ;;
        *.tar.bz2|*.tbz2)       spinner_start "解压 TAR.BZ2…";        tar -xjf "$INPUT_PATH" -C "$WORK_DIR";   rc=$?; spinner_stop ;;
        *.tar)                  spinner_start "解压 TAR…";            tar -xf  "$INPUT_PATH" -C "$WORK_DIR";   rc=$?; spinner_stop ;;
        *.7z)                   spinner_start "解压 7Z…";             7z x -y "$INPUT_PATH" -o"$WORK_DIR" >/dev/null; rc=$?; spinner_stop ;;
        *)
            # 扩展名不认识时，按文件头 magic number 兜底识别（下载文件常丢扩展名）
            magic=$(head -c 6 "$INPUT_PATH" 2>/dev/null | od -An -tx1 | tr -d ' \n')
            case "$magic" in
                504b0304*|504b0506*|504b0708*)
                    log warn "扩展名未知，按 ZIP 文件头识别"
                    spinner_start "解压 ZIP…"; unzip -q -o "$INPUT_PATH" -d "$WORK_DIR"; rc=$?; spinner_stop ;;
                1f8b*)
                    log warn "扩展名未知，按 GZIP 文件头识别"
                    spinner_start "解压 TAR.GZ…"; tar -xzf "$INPUT_PATH" -C "$WORK_DIR"; rc=$?; spinner_stop ;;
                fd377a585a00*)
                    log warn "扩展名未知，按 XZ 文件头识别"
                    spinner_start "解压 TAR.XZ…"; tar -xJf "$INPUT_PATH" -C "$WORK_DIR"; rc=$?; spinner_stop ;;
                377abcaf271c*)
                    log warn "扩展名未知，按 7Z 文件头识别"
                    spinner_start "解压 7Z…"; 7z x -y "$INPUT_PATH" -o"$WORK_DIR" >/dev/null; rc=$?; spinner_stop ;;
                526172211a07*)
                    log warn "扩展名未知，按 RAR 文件头识别"
                    command -v unrar >/dev/null 2>&1 || install_pkg_if_missing "unrar" "unrar"
                    spinner_start "解压 RAR…"; unrar x -y "$INPUT_PATH" "$WORK_DIR/" >/dev/null; rc=$?; spinner_stop ;;
                *)
                    log err "不支持的压缩格式（扩展名与文件头均无法识别）"; exit 1 ;;
            esac
            ;;
    esac
    if [ "$rc" -ne 0 ]; then log err "解压失败"; exit 1; fi
    log ok "解压完成"
else
    WORK_DIR="$INPUT_PATH"
    [ -r "$WORK_DIR" ] || { log err "目录不可读"; exit 1; }
    log ok "使用已解压目录"
fi
rm -rf "$WORK_DIR/__MACOSX" 2>/dev/null
spinner_start "清理 Windows 换行符 (CRLF)…"
strip_crlf "$WORK_DIR"
spinner_stop
log ok "CRLF 清理完成"
step_done

# =====================================================================
# [STEP 4/8] 根目录探测
# =====================================================================
step_begin 4 "探测根目录"
stage_title "4/8" "项目根目录与类型识别" "$I_STAR"
PROJECT_DIR=""; BUILD_TYPE="UNKNOWN"; HIT_FILE=""

spinner_start "扫描项目结构…"
for spec in "settings.gradle:GRADLE" "settings.gradle.kts:GRADLE" "gradlew:GRADLE" \
            "build.gradle:GRADLE" "build.gradle.kts:GRADLE" \
            "pom.xml:MAVEN" "build.xml:ANT" "build.sh:SHELL" "build_linux.sh:SHELL" \
            "CMakeLists.txt:NDK" "Android.mk:NDK" "configure:MAKE"; do
    f=$(find_shallowest "$WORK_DIR" "${spec%%:*}")
    if [ -n "$f" ]; then HIT_FILE="$f"; BUILD_TYPE="${spec##*:}"; break; fi
done
spinner_stop

if [ -n "$HIT_FILE" ]; then
    PROJECT_DIR=$(dirname "$HIT_FILE")
    log ok "命中 ${HIT_FILE##*/} → 类型 ${BUILD_TYPE}"
fi

if [ -z "$PROJECT_DIR" ]; then
    log err "未能识别项目结构，顶层内容如下："
    ls -la "$WORK_DIR"
    exit 1
fi

cd "$PROJECT_DIR" || { log err "无法进入项目目录"; exit 1; }
log ok "项目根目录 ${PROJECT_DIR}"
log star "项目类型   ${BUILD_TYPE}"

# 框架型项目提示（Flutter / RN）：只构建其中 Android 原生工程，避免误导
FW_HINT=""
[ -n "$(find_shallowest "$WORK_DIR" 'pubspec.yaml')" ] && FW_HINT="Flutter"
[ -z "$FW_HINT" ] && [ -n "$(find_shallowest "$WORK_DIR" 'package.json')" ] && FW_HINT="React Native / Node"
if [ -n "$FW_HINT" ]; then
    log warn "检测到 ${FW_HINT} 框架，本工具仅构建其中的 Android 原生工程"
    log info "若失败通常是缺 Dart / Node 工具链，非本脚本可控"
fi

strip_crlf "$PROJECT_DIR"
step_done

# =====================================================================
# [STEP 5/8] 环境准备
# =====================================================================
step_begin 5 "环境准备"
stage_title "5/8" "按项目推导构建参数" "$I_ROCKET"

GRADLE_MAJOR=$(detect_gradle_version)
if [ -n "$GRADLE_MAJOR" ]; then log info "Gradle 主版本 = ${GRADLE_MAJOR}"; else log info "Gradle 主版本 = 未知"; fi

if [ -n "$GRADLE_MAJOR" ] && [ "$GRADLE_MAJOR" -lt 8 ] 2>/dev/null; then
    install_pkg_if_missing "openjdk-17" "java"
else
    install_pkg_if_missing "openjdk-21" "java"
fi
refresh_java_home

# ★ 修复：只有"真能用"的 gradlew 才跳过安装 gradle。
#   很多工程（含本工具生成的）带 gradlew 却没有 gradle-wrapper.jar，
#   那样 ./gradlew 必然失败且不会自动装 gradle → 卡死在 STEP 6。
if [ "$BUILD_TYPE" = "GRADLE" ] && ! gradlew_usable; then
    log warn "gradlew 不完整（缺 gradle-wrapper.jar），改用系统 gradle" 1
    install_pkg_if_missing "gradle" "gradle"
fi

if [ "$BUILD_TYPE" = "GRADLE" ] || [ "$BUILD_TYPE" = "SHELL" ] || [ "$BUILD_TYPE" = "ANT" ]; then
    CS=$(detect_compile_sdk)
    log info "compileSdk = ${CS}"

    # ★ 修复：平台和 build-tools 分开各自保障。
    #   原逻辑把 build-tools 安装塞在"平台缺失"分支里，平台已装时就完全跳过，
    #   导致 build-tools 缺失时一路走到编译才炸。
    if [ -d "$ANDROID_HOME/platforms/android-$CS" ]; then
        log ok "android-$CS 平台已就位" 1
    elif command -v sdkmanager &>/dev/null; then
        spinner_start "安装 android-$CS 平台…"
        yes | sdkmanager --licenses >/dev/null 2>&1
        sdkmanager "platforms;android-$CS" >/dev/null 2>&1
        spinner_stop
        if [ -d "$ANDROID_HOME/platforms/android-$CS" ]; then
            log ok "android-$CS 平台安装完成" 1
        else
            log warn "android-$CS 平台安装失败" 1
        fi
    else
        log warn "无 sdkmanager，跳过平台安装" 1
    fi

    # build-tools：无论平台是否已就位都要确认，并把项目里的 buildToolsVersion 对齐到实际版本
    ensure_build_tools
fi

# ★ 修复：externalNativeBuild 用 CMake 时，AGP 要求 SDK 里装有对应版本的 CMake，
#   否则报 "CMake '3.22.1' was not found in SDK"。这里自动补装。
if [ "$BUILD_TYPE" = "GRADLE" ] && grep -rqE "externalNativeBuild" \
        --include="*.gradle" --include="*.gradle.kts" . 2>/dev/null; then
    CMV=$(detect_cmake_version)
    log info "检测到 externalNativeBuild，CMake 版本 = ${CMV}" 1
    if [ ! -d "$ANDROID_HOME/cmake/$CMV" ]; then
        if command -v sdkmanager &>/dev/null; then
            spinner_start "安装 cmake-${CMV}…"
            yes | sdkmanager --licenses >/dev/null 2>&1
            sdkmanager "cmake;$CMV" >/dev/null 2>&1
            spinner_stop
            if [ -d "$ANDROID_HOME/cmake/$CMV" ]; then
                log ok "cmake-${CMV} 安装完成" 1
            else
                # 兜底：SDK 里已有别的 CMake 版本就改用它，避免 "CMake x.y.z was not found"
                HAVE=$(ls -1 "$ANDROID_HOME/cmake" 2>/dev/null | sort -V | tail -1)
                if [ -n "$HAVE" ]; then
                    log warn "cmake-${CMV} 装不到，改用已装的 ${HAVE}" 1
                    for gf in $(find . -maxdepth 3 -name "build.gradle" -o -maxdepth 3 -name "build.gradle.kts" 2>/dev/null); do
                        sed -i "s/version[[:space:]]*\"${CMV}\"/version \"${HAVE}\"/g" "$gf" 2>/dev/null
                    done
                else
                    log warn "SDK 无可用 CMake，native 构建会失败；手动：sdkmanager --install 'cmake;$CMV'" 1
                fi
            fi
        else
            log warn "无 sdkmanager，跳过 CMake 安装" 1
        fi
    else
        log ok "cmake-${CMV} 已就位" 1
    fi
fi

AAPT2_BIN=$(detect_aapt2)
mkdir -p "$HOME/.gradle"
if [ -n "$AAPT2_BIN" ] && ! grep -q "aapt2FromMavenOverride" "$HOME/.gradle/gradle.properties" 2>/dev/null; then
    echo "android.aapt2FromMavenOverride=$AAPT2_BIN" >> "$HOME/.gradle/gradle.properties"
    log ok "aapt2 覆盖已写入 ${AAPT2_BIN}"
fi
step_done

# =====================================================================
# [STEP 6/8] 构建
# =====================================================================
step_begin 6 "执行构建"
stage_title "6/8" "编译打包（实时日志流）" "$I_FIRE"
XMX=$(calc_xmx)
log info "推导 Gradle 堆内存 -Xmx${XMX}"
printf "  %s%s%s %s%s 编译中，日志实时滚动…%s\n" "${BG_ORANGE}" "${HWHITE}" " BUILD " "${R}" "${BOLD}${P_ORANGE}${I_FIRE}" "${R}" >&3
rule "$P_DEEP" >&3

BUILD_RC=0
case "$BUILD_TYPE" in

  GRADLE)
    [ -f "local.properties" ] || : > local.properties
    sed -i '/^[[:space:]]*sdk\.dir=/d;/^[[:space:]]*ndk\.dir=/d' local.properties 2>/dev/null
    { echo "sdk.dir=$ANDROID_HOME"; echo "ndk.dir=$ANDROID_NDK_HOME"; } >> local.properties
    [ -f "gradle.properties" ] || : > gradle.properties
    sed -i '/^[[:space:]]*org\.gradle\.jvmargs=/d' gradle.properties 2>/dev/null
    echo "org.gradle.jvmargs=-Xmx$XMX -Dfile.encoding=UTF-8" >> gradle.properties
    # 修复 AGP 7+ noCompress 多参数兼容性问题
    if grep -qE "noCompress" app/build.gradle 2>/dev/null; then
        if grep -qE "noCompress.*'.*','.+'" app/build.gradle 2>/dev/null; then
            sed -i "s/noCompress '\([^']*\)', '\([^']*\)'/noCompress '\1'\n            noCompress '\2'/g" app/build.gradle 2>/dev/null
            log ok "已修复 app/build.gradle noCompress API 兼容性" 1
        fi
    fi
    chmod +x gradlew 2>/dev/null
    if gradlew_usable; then
        RUNNER="./gradlew"
        log ok "使用项目自带 gradlew" 1
    elif command -v gradle &>/dev/null; then
        RUNNER="gradle"
        # 项目里的 wrapper 配置可能对不上已装的 gradle，构建时以系统版为准
        [ -f "gradle/wrapper/gradle-wrapper.jar" ] || log info "wrapper jar 缺失，走系统 gradle" 1
    else
        log err "既无可用 gradlew 也找不到 gradle 命令"
        exit 1
    fi
    run_stream "$RUNNER assembleDebug --no-daemon" "$RUNNER" assembleDebug --no-daemon
    BUILD_RC=$?
    if [ "$BUILD_RC" -ne 0 ]; then
        # ★ 先尝试 DSL 自愈：注释掉与当前 AGP 不兼容的配置行后重跑一次
        if heal_gradle_dsl; then
            log warn "检测到 DSL 不兼容，已自动修正，重新构建…"
            run_stream "$RUNNER assembleDebug --no-daemon" "$RUNNER" assembleDebug --no-daemon
            BUILD_RC=$?
        fi
    fi
    if [ "$BUILD_RC" -ne 0 ]; then
        log warn "主构建失败，降级重试（跳过 lint / test）"
        run_stream "$RUNNER assembleDebug -x lint -x test" "$RUNNER" assembleDebug --no-daemon -x lint -x lintVitalRelease -x test
        BUILD_RC=$?
    fi
    if [ "$BUILD_RC" -ne 0 ]; then
        log warn "无 debug 变体，尝试 assembleRelease（产出多为未签名包，稍后自动补签）"
        run_stream "$RUNNER assembleRelease" "$RUNNER" assembleRelease --no-daemon -x lint -x test
        BUILD_RC=$?
    fi
    ;;

  SHELL)
    SHELL_SCRIPT="build.sh"
    [ -f "build_linux.sh" ] && SHELL_SCRIPT="build_linux.sh"
    BT_VER=$(detect_build_tools_ver)
    cp "$SHELL_SCRIPT" "$SHELL_SCRIPT.bak" 2>/dev/null
    sed -i "s|build-tools/[0-9][0-9.]*|build-tools/$BT_VER|g" "$SHELL_SCRIPT" 2>/dev/null
    sed -i 's|\bdx --dex|d8|g' "$SHELL_SCRIPT" 2>/dev/null
    sed -i 's|^\([[:space:]]*\)mkdir\( -p\)\?[[:space:]]*/root.*|true # skip /root|g' "$SHELL_SCRIPT" 2>/dev/null
    chmod +x "$SHELL_SCRIPT"
    # ★ 修复：给原生编译准备干净环境。
    #   若环境里 PATH 曾混进 NDK toolchain，clang++ 会被解析成 NDK 的，
    #   裸跑缺 --sysroot → fatal error: 'arpa/inet.h' file not found。
    setup_host_compiler
    log info "CXX=${CXX:-<默认>}  CC=${CC:-<默认>}" 1
    run_stream "bash $SHELL_SCRIPT" bash "$SHELL_SCRIPT"
    BUILD_RC=$?
    ;;

  ANT)
    run_stream "ant debug" ant debug
    BUILD_RC=$?
    if [ "$BUILD_RC" -ne 0 ]; then
        log warn "ant debug 失败，尝试 ant release"
        run_stream "ant release" ant release
        BUILD_RC=$?
    fi
    ;;

  MAVEN)
    run_stream "mvn package -DskipTests" mvn -q package -DskipTests
    BUILD_RC=$?
    ;;

  NDK)
    if [ -f "CMakeLists.txt" ]; then
        mkdir -p build && cd build
        run_stream "cmake (Android toolchain)" cmake .. \
            -DCMAKE_TOOLCHAIN_FILE="$ANDROID_NDK_HOME/build/cmake/android.toolchain.cmake" \
            -DANDROID_ABI="${ANDROID_ABI:-arm64-v8a}" \
            -DANDROID_PLATFORM="android-$(detect_compile_sdk)"
        BUILD_RC=$?
        if [ "$BUILD_RC" -eq 0 ]; then
            run_stream "make -j" make -j"$(nproc 2>/dev/null || echo 2)"
            BUILD_RC=$?
        fi
        cd ..
    else
        run_stream "ndk-build" "$ANDROID_NDK_HOME/ndk-build"
        BUILD_RC=$?
    fi
    ;;

  MAKE)
    if [ -f "configure" ]; then run_stream "./configure" ./configure --prefix="$PREFIX"; fi
    run_stream "make -j" make -j"$(nproc 2>/dev/null || echo 2)"
    BUILD_RC=$?
    ;;
esac

rule "$P_DEEP" >&3
if [ "$BUILD_RC" -ne 0 ]; then
    log err "构建失败，完整日志: $LOG_FILE"
    diagnose_build_failure
    exit 1
fi
log ok "构建流程结束"
step_done

# =====================================================================
# [STEP 7/8] 产物搜索
# =====================================================================
step_begin 7 "搜索产物"
stage_title "7/8" "搜索构建产物" "$I_PKG"
ART=()
# 如果是 SHELL 项目，额外搜索编译产物（二进制文件）
    if [ "$BUILD_TYPE" = "SHELL" ]; then
        while IFS= read -r f; do
            [ -n "$f" ] && ART+=("$f")
        done < <(
            find "$PROJECT_DIR" -maxdepth 5 -type f -perm /111 \
                ! -name "*.so" ! -name "*.o" ! -name "*.d" \
                ! -name "*.c" ! -name "*.h" ! -name "*.hpp" \
                ! -name "*.cpp" ! -name "*.cc" \
                ! -name "*.gradle" ! -name "*.xml" ! -name "*.json" \
                ! -name "*.yml" ! -name "*.yaml" ! -name "*.toml" \
                ! -name "*.sh" ! -name "*.bat" \
                ! -name "CMakeLists.txt" ! -name "Makefile" ! -name "*.mk" \
                2>/dev/null | sort -rn | head -10
        )
        [ ${#ART[@]} -gt 0 ] && log ok "SHELL 产物搜索完成，发现 ${#ART[@]} 个候选文件" 1
    fi


spinner_start "扫描 apk / aar…"
while IFS= read -r f; do [ -n "$f" ] && ART+=("$f"); done < <(
    find "$PROJECT_DIR" -type f \( -name "*.apk" -o -name "*.aar" \) \
         ! -name "*unaligned*" ! -path "*/intermediates/*" \
         -printf '%T@\t%p\n' 2>/dev/null | sort -rn | cut -f2-
)
spinner_stop

if [ ${#ART[@]} -eq 0 ]; then
    spinner_start "扫描 so / jar / a…"
    while IFS= read -r f; do [ -n "$f" ] && ART+=("$f"); done < <(
        find "$PROJECT_DIR" -type f \( -name "*.so" -o -name "*.jar" -o -name "*.a" \) \
             \( -path "*/outputs/*" -o -path "*/libs/*" -o -path "*/obj/local/*" -o -path "*/lib/*" \) \
             -printf '%T@\t%p\n' 2>/dev/null | sort -rn | cut -f2- | head -20
    )
    spinner_stop
fi

if [ ${#ART[@]} -eq 0 ]; then
    log err "未找到任何构建产物，详见日志: $LOG_FILE"
    exit 1
fi

log ok "发现 ${#ART[@]} 个产物"
if [ ${#ART[@]} -gt 1 ]; then
    printf "\n  %s%s%s 请选择要保存的产物：%s\n" "${BG_VIOLET}" "${HWHITE}" " SELECT " "${R}" >&3
    n=1
    for a in "${ART[@]}"; do
        sz=$(du -h "$a" 2>/dev/null | cut -f1)
        printf "  %s%2d%s %s%s%s  %s%s%s  %s%s%s\n" \
            "${P_GOLD}" "${n}" "${R}" "${P_VIOLET}" "${I_ARROW}" "${R}" \
            "${HWHITE}" "$(basename "$a")" "${R}" "${DIM}" "(${sz})" "${R}" >&3
        n=$((n+1))
    done
    printf "\n" >&3
    ART_PATH=""
    while [ -z "$ART_PATH" ]; do
        printf "  %s%s 输入序号 (回车默认 1):%s " "${BOLD}${P_MINT}" "▸" "${R}" >&3
        read -r -e choice
        [ -z "${choice:-}" ] && choice=1
        if [[ "$choice" =~ ^[0-9]+$ ]] && [ "$choice" -ge 1 ] && [ "$choice" -le ${#ART[@]} ]; then
            ART_PATH="${ART[$((choice-1))]}"
        else
            printf "  %s%s 序号无效%s\n" "${HYELLOW}" "${I_WARN}" "${R}" >&3
        fi
    done
else
    ART_PATH="${ART[0]}"
fi
post_process_apk "$ART_PATH"
sz=$(du -h "$ART_PATH" 2>/dev/null | cut -f1)
log star "选中 $(basename "$ART_PATH") (${sz})"
step_done

# =====================================================================
# [STEP 8/8] 保存
# =====================================================================
step_begin 8 "保存产物"
stage_title "8/8" "保存产物" "$I_SAVE"

pick_save_dir() {
    for d in "$HOME/storage/downloads" "$HOME/storage/shared/Download" \
             "/storage/emulated/0/Download" "$HOME/Download" "$HOME"; do
        if mkdir -p "$d" 2>/dev/null && : > "$d/.__wtest" 2>/dev/null; then
            rm -f "$d/.__wtest" 2>/dev/null; echo "$d"; return 0
        fi
    done
    echo "$HOME"
}
DEFAULT_DIR=$(pick_save_dir)
printf "  %s%s%s %s%s保存目录%s %s(回车默认 %s)%s: " \
    "${BOLD}${P_MINT}" "▸" "${R}" "${BOLD}" "${HWHITE}" "${R}" "${P_GREY}" "${DEFAULT_DIR}" "${R}" >&3
read -r -e CUSTOM_DIR
CUSTOM_DIR="${CUSTOM_DIR:-$DEFAULT_DIR}"
CUSTOM_DIR="${CUSTOM_DIR/#\~/$HOME}"
if ! ( mkdir -p "$CUSTOM_DIR" 2>/dev/null && : > "$CUSTOM_DIR/.__wtest" 2>/dev/null ); then
    log warn "目标目录不可写，回退到 ${DEFAULT_DIR}"
    CUSTOM_DIR="$DEFAULT_DIR"
fi
rm -f "$CUSTOM_DIR/.__wtest" 2>/dev/null

BASE=$(basename "$ART_PATH")
TARGET="$CUSTOM_DIR/$BASE"
if [ -e "$TARGET" ]; then
    TARGET="$CUSTOM_DIR/${BASE%.*}_$(date +"%Y%m%d_%H%M%S").${BASE##*.}"
    log warn "同名已存在，另存为 $(basename "$TARGET")"
fi

spinner_start "写入文件…"
cp "$ART_PATH" "$TARGET" 2>/dev/null
CP_RC=$?
spinner_stop
step_done
printf "\n" >&3

# =====================================================================
# 结束汇总卡片
# =====================================================================
END_TS=$(date +%s)
COST=$(( END_TS - START_TS ))
COST_TXT="$(( COST / 60 )) 分 $(( COST % 60 )) 秒"
FSIZE=$(du -h "$TARGET" 2>/dev/null | cut -f1)

if [ "$CP_RC" -ne 0 ]; then
    printf "  %s%s%s %s%s 保存失败：目标目录不可写%s\n" \
        "${BG_RED}" "${HWHITE}" " FAILED " "${R}" "${HRED}${I_ERR}" "${R}" >&3
    # 难过表情：眼睛变 ×，嘴角朝下
    mini_face "$EYE_SAD" "╭───╮"
    printf "  %s%s%s\n\n" "${DIM}${P_GREY}" "  ${USER_NAME}，别急，看看日志：${LOG_FILE}" "${R}" >&3
    log err "保存失败"
    exit 1
fi

read_apk_info "$TARGET"

# 汇总卡片：上下用定宽 ASCII 框线，内容行不封右边框（彻底规避中文宽度错位）
printf "\n" >&3
printf "  %s%s%s\n" "${P_LIME}" "$(repeat_ch 44 '═')" "${R}" >&3
printf "  %s%s%s %s%s BUILD SUCCESS%s\n" \
    "${BG_GREEN}" "${BOLD}${HWHITE}" " ${I_OK} " "${R}" "${P_LIME}${BOLD}" "${R}" >&3
printf "  %s%s%s\n" "${P_LIME}" "$(repeat_ch 44 '═')" "${R}" >&3
printf "  %s %s%s%s\n" "${P_MINT}${I_PKG}${R}" "${BOLD}${HWHITE}" "$(basename "$TARGET")" "${R}" >&3
if [ -n "$APK_PKG" ]; then
    printf "  %s %s%s%s\n" "${P_PINK}${I_STAR}${R}" "${P_GREY}" "包名 ${APK_PKG}" "${R}" >&3
    sdkline=""
    [ -n "$APK_MINSDK" ] && sdkline="   SDK ${APK_MINSDK} → ${APK_TGTSDK}"
    printf "  %s %s%s%s\n" "${P_PINK}${I_DOT}${R}"  "${P_GREY}" \
        "版本 ${APK_VNAME} (${APK_VCODE})${sdkline}" "${R}" >&3
fi
printf "  %s %s%s%s\n" "${P_SKY}${I_FOLDER}${R}"  "${P_GREY}" "$TARGET" "${R}" >&3
printf "  %s %s%s · %s%s\n" "${P_GOLD}${I_TIME}${R}" "${P_GREY}" "$COST_TXT" "$FSIZE" "${R}" >&3
printf "  %s %s%s%s\n" "${P_VIOLET}${I_LOG}${R}"   "${P_GREY}" "$LOG_FILE" "${R}" >&3
printf "  %s%s%s\n" "${P_LIME}" "$(repeat_ch 44 '═')" "${R}" >&3

# 庆祝：机器人换成眯眼笑的表情
printf "\n" >&3
mini_face "$EYE_HAPPY" "╰───╯"
printf "  %s%s%s\n" "${DIM}${P_GREY}"   "   ${USER_NAME}，搞定收工！"        "${R}" >&3
printf "\n" >&3

printf "  %s%s%s %s%s 打包完成，可以安装了%s\n\n" \
    "${BG_VIOLET}" "${HWHITE}" " DONE " "${R}" "${BOLD}${P_PINK}${I_SPARK}" "${R}" >&3

log star "构建成功：$TARGET"

# 打包次数 +1 并回写配置（下次启动就能报数）
BUILD_COUNT=$(( BUILD_COUNT + 1 ))
save_conf

command -v termux-toast   >/dev/null 2>&1 && termux-toast   -g bottom "打包完成：$(basename "$TARGET")" 2>/dev/null
command -v termux-vibrate >/dev/null 2>&1 && termux-vibrate -d 120 2>/dev/null

exit 0