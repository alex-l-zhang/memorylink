import type { Departed, HouseStyle, Letter, Member, Photo, Scene, Skyline, Story, TimeOfDay } from './data'

export function mix(a: string, b: string, t: number): string {
  const pa = parse(a)
  const pb = parse(b)
  const k = Math.max(0, Math.min(1, t))
  const r = Math.round(pa[0] + (pb[0] - pa[0]) * k)
  const g = Math.round(pa[1] + (pb[1] - pa[1]) * k)
  const bl = Math.round(pa[2] + (pb[2] - pa[2]) * k)
  return `#${hex(r)}${hex(g)}${hex(bl)}`
}

function parse(value: string): [number, number, number] {
  const v = value.replace('#', '')
  return [
    parseInt(v.slice(0, 2), 16),
    parseInt(v.slice(2, 4), 16),
    parseInt(v.slice(4, 6), 16),
  ]
}

function hex(n: number): string {
  return n.toString(16).padStart(2, '0')
}

const TIME_TINT: Record<TimeOfDay, { a: string; b: string; k: number; label: string; hint: string }> = {
  dawn: { a: '#ffd9a0', b: '#ffc48f', k: 0.08, label: '清晨', hint: '光刚起来，水面上有一层薄雾' },
  dusk: { a: '#ff9d5c', b: '#e2604f', k: 0.46, label: '黄昏', hint: '太阳落到山背后，屋里该点灯了' },
  night: { a: '#0b1b3a', b: '#16233f', k: 0.8, label: '夜晚', hint: '院子安静下来，只剩一盏灯亮着' },
}

function palette(scene: Scene, time: TimeOfDay) {
  const t = TIME_TINT[time]
  const k = t.k
  return {
    sky0: mix(scene.sky[0], t.a, k),
    sky1: mix(scene.sky[1], t.b, k),
    far: mix(scene.far, t.a, k * 0.85),
    mid: mix(scene.mid, t.b, k * 0.85),
    ground: mix(scene.ground, t.b, k * 0.7),
    water: mix(mix(scene.ground, scene.sky[1], 0.45), t.a, k * 0.9),
    wall: mix(scene.sky[1], '#ffffff', 0.35),
    night: time === 'night',
    warm: mix('#ffcf85', scene.accent, 0.35),
  }
}

type SceneProps = {
  scene: Scene
  time: TimeOfDay
  memorial: boolean
  members: Member[]
  photos: Photo[]
  stories: Story[]
  letters: Letter[]
  departed: Departed[]
}

/** 外景：位置 + 房型 + 时间 + 内容联动。 */
export function SceneView(props: SceneProps) {
  const { scene, time, memorial, members, stories, letters, departed } = props
  const c = palette(scene, time)
  const lit = Math.min(members.length + 1, 4)
  const glow = Math.min(stories.length / 8, 1)
  const stars = time === 'night'

  return (
    <svg className="scene" viewBox="0 0 1000 560" role="img" aria-label={`${scene.name} 外景`}>
      <defs>
        <linearGradient id="sky" x1="0" y1="0" x2="0" y2="1">
          <stop offset="0%" stopColor={c.sky0} />
          <stop offset="100%" stopColor={c.sky1} />
        </linearGradient>
        <linearGradient id="water" x1="0" y1="0" x2="0" y2="1">
          <stop offset="0%" stopColor={c.water} />
          <stop offset="100%" stopColor={mix(c.water, '#ffffff', 0.25)} />
        </linearGradient>
        <radialGradient id="glow">
          <stop offset="0%" stopColor="#ffd79a" stopOpacity="0.9" />
          <stop offset="100%" stopColor="#ffd79a" stopOpacity="0" />
        </radialGradient>
        <radialGradient id="lampGlow">
          <stop offset="0%" stopColor="#ffbe63" stopOpacity="0.95" />
          <stop offset="100%" stopColor="#ffbe63" stopOpacity="0" />
        </radialGradient>
      </defs>

      <rect width="1000" height="560" fill="url(#sky)" />

      {stars &&
        Array.from({ length: 34 }).map((_, i) => (
          <circle
            key={i}
            className="star"
            cx={(i * 137) % 980 + 10}
            cy={(i * 71) % 200 + 16}
            r={i % 5 === 0 ? 1.8 : 1.1}
            fill="#ffffff"
            opacity={0.7}
            style={{ animationDelay: `${(i % 7) * 0.4}s` }}
          />
        ))}

      <Celestial time={time} accent={scene.accent} />

      <g className="clouds">
        <Cloud x={120} y={90} scale={1} className="cloud-a" tint={c.sky1} />
        <Cloud x={520} y={60} scale={0.75} className="cloud-b" tint={c.sky1} />
        <Cloud x={790} y={116} scale={0.9} className="cloud-a" tint={c.sky1} />
      </g>

      <Skyline kind={scene.skyline} far={c.far} mid={c.mid} night={c.night} />

      {scene.water && (
        <>
          <rect y="392" width="1000" height="70" fill="url(#water)" />
          <g className="shimmer" opacity="0.55">
            {[0, 1, 2, 3, 4, 5].map((i) => (
              <rect key={i} x={80 + i * 150} y={404 + (i % 3) * 14} width="120" height="2.5" rx="1.2" fill="#ffffff" opacity={0.5} />
            ))}
          </g>
        </>
      )}

      <rect y="452" width="1000" height="108" fill={c.ground} />
      <ellipse cx="500" cy="470" rx="620" ry="42" fill={mix(c.ground, '#000000', 0.08)} />

      {departed.map((person, index) => (
        <Tree key={person.id} x={132 + index * 84} y={452} night={c.night} />
      ))}

      <House
        style={scene.house}
        x={598}
        baseY={462}
        width={236}
        litWindows={lit}
        studyGlow={glow}
        accent={scene.accent}
        night={c.night}
        warm={c.warm}
      />

      {memorial && departed.length > 0 && <DoorLamp x={660} baseY={462} />}

      <Nameplate
        x={852}
        y={352}
        household="陈家"
        family={members.length + 1}
        photos={props.photos.length}
        letters={letters.length}
      />

      <g className="fence">
        {Array.from({ length: 14 }).map((_, i) => (
          <rect key={i} x={40 + i * 26} y={462} width="4" height="26" rx="1.5" fill={mix(c.ground, '#000000', 0.28)} />
        ))}
        <rect x={40} y={470} width="342" height="4" rx="2" fill={mix(c.ground, '#000000', 0.32)} />
      </g>
    </svg>
  )
}

function Celestial({ time, accent }: { time: TimeOfDay; accent: string }) {
  if (time === 'night') {
    return (
      <g>
        <circle cx="812" cy="96" r="34" fill="#f4f1e4" />
        <circle cx="800" cy="88" r="30" fill="#e9e6d8" opacity="0.5" />
      </g>
    )
  }
  const cx = time === 'dawn' ? 214 : 742
  const cy = time === 'dawn' ? 186 : 128
  return (
    <g>
      <circle cx={cx} cy={cy} r="86" fill="url(#glow)" />
      <circle cx={cx} cy={cy} r="30" fill={time === 'dawn' ? '#ffe6b8' : '#ffc98a'} />
      <circle cx={cx} cy={cy} r="30" fill={accent} opacity="0.12" />
    </g>
  )
}

function Cloud({ x, y, scale, className, tint }: { x: number; y: number; scale: number; className: string; tint: string }) {
  return (
    <g className={className} transform={`translate(${x} ${y}) scale(${scale})`} opacity="0.85">
      <ellipse cx="0" cy="0" rx="62" ry="20" fill="#ffffff" opacity="0.9" />
      <ellipse cx="-34" cy="6" rx="38" ry="15" fill={tint} opacity="0.55" />
      <ellipse cx="30" cy="7" rx="42" ry="16" fill="#ffffff" opacity="0.75" />
    </g>
  )
}

function Skyline({ kind, far, mid, night }: { kind: Skyline; far: string; mid: string; night: boolean }) {
  const windowColor = night ? '#ffd08a' : '#ffffff'
  if (kind === 'mountain') {
    return (
      <g>
        <polygon points="0,452 190,232 360,452" fill={far} />
        <polygon points="150,452 400,176 660,452" fill={mid} />
        <polygon points="520,452 740,262 1000,452" fill={far} />
        <polygon points="368,214 400,176 432,214 416,224 384,224" fill="#ffffff" opacity="0.85" />
        <polygon points="714,296 740,262 766,296 752,304 728,304" fill="#ffffff" opacity="0.75" />
      </g>
    )
  }
  if (kind === 'forest') {
    return (
      <g>
        {Array.from({ length: 16 }).map((_, i) => (
          <polygon
            key={i}
            points={`${i * 68 - 20},452 ${i * 68 + 14},${300 + (i % 4) * 22} ${i * 68 + 48},452`}
            fill={i % 2 === 0 ? far : mid}
          />
        ))}
      </g>
    )
  }
  if (kind === 'city') {
    return (
      <g>
        {Array.from({ length: 18 }).map((_, i) => {
          const h = 90 + ((i * 53) % 150)
          const w = 44 + ((i * 17) % 26)
          return (
            <g key={i}>
              <rect x={i * 58 - 10} y={452 - h} width={w} height={h} fill={i % 3 === 0 ? far : mid} />
              {Array.from({ length: Math.floor(h / 34) }).map((_, j) => (
                <rect
                  key={j}
                  x={i * 58 + 2}
                  y={452 - h + 14 + j * 32}
                  width="10"
                  height="12"
                  fill={windowColor}
                  opacity={(i + j) % 3 === 0 ? 0.85 : 0.25}
                />
              ))}
            </g>
          )
        })}
      </g>
    )
  }
  if (kind === 'village') {
    return (
      <g>
        {Array.from({ length: 6 }).map((_, i) => (
          <g key={i}>
            <polygon points={`${i * 170 + 10},452 ${i * 170 + 76},366 ${i * 170 + 142},452`} fill={i % 2 === 0 ? far : mid} />
          </g>
        ))}
      </g>
    )
  }
  // lake / sea：远山与海平线
  return (
    <g>
      <polygon points="0,392 150,286 320,392" fill={far} opacity="0.9" />
      <polygon points="230,392 420,258 640,392" fill={mid} opacity="0.85" />
      <polygon points="560,392 760,300 1000,392" fill={far} opacity="0.8" />
    </g>
  )
}

function Tree({ x, y, night }: { x: number; y: number; night: boolean }) {
  const leaf = night ? '#2f4a3c' : '#4f7a52'
  return (
    <g transform={`translate(${x} ${y})`}>
      <rect x="-4" y="-34" width="8" height="34" rx="3" fill={night ? '#3a2f26' : '#6b4b31'} />
      <circle cx="0" cy="-52" r="26" fill={leaf} />
      <circle cx="-18" cy="-38" r="18" fill={leaf} opacity="0.92" />
      <circle cx="18" cy="-40" r="17" fill={leaf} opacity="0.88" />
    </g>
  )
}

function DoorLamp({ x, baseY }: { x: number; baseY: number }) {
  return (
    <g transform={`translate(${x} ${baseY})`}>
      <circle className="lamp-halo" cx="0" cy="-96" r="54" fill="url(#lampGlow)" />
      <rect x="-2.5" y="-96" width="5" height="30" fill="#4a3a2c" />
      <path d="M-14 -96 h28 l-6 -18 h-16 z" fill="#4a3a2c" />
      <path className="flame" d="M0 -100 c7 8 5 14 0 18 c-5 -4 -7 -10 0 -18" fill="#ffd58a" />
    </g>
  )
}

function Nameplate({ x, y, household, family, photos, letters }: { x: number; y: number; household: string; family: number; photos: number; letters: number }) {
  return (
    <g transform={`translate(${x} ${y})`}>
      <rect x="0" y="0" width="124" height="82" rx="10" fill="#fdf8ee" stroke="#c8ab84" strokeWidth="2" />
      <text x="62" y="28" textAnchor="middle" fontSize="19" fill="#4a3826" fontWeight="700">
        {household}
      </text>
      <text x="62" y="48" textAnchor="middle" fontSize="12" fill="#8a7157">
        家人 {family} · 照片 {photos}
      </text>
      <text x="62" y="66" textAnchor="middle" fontSize="12" fill="#8a7157">
        留言 {letters}
      </text>
    </g>
  )
}

type HouseProps = {
  style: HouseStyle
  x: number
  baseY: number
  width: number
  litWindows: number
  studyGlow: number
  accent: string
  night: boolean
  warm: string
}

function Window({ x, y, w, h, lit, warm }: { x: number; y: number; w: number; h: number; lit: boolean; warm: string }) {
  return (
    <g>
      {lit && <rect x={x - 6} y={y - 6} width={w + 12} height={h + 12} rx="6" fill={warm} opacity="0.45" />}
      <rect x={x} y={y} width={w} height={h} rx="3" fill={lit ? warm : '#2f3b46'} opacity={lit ? 1 : 0.75} stroke="#ffffff" strokeOpacity="0.35" />
      <line x1={x + w / 2} y1={y} x2={x + w / 2} y2={y + h} stroke="#ffffff" strokeOpacity="0.4" />
    </g>
  )
}

function House(props: HouseProps) {
  const { style, x, baseY, width: w, litWindows, studyGlow, accent, night, warm } = props
  const h = 150
  const y = baseY - h
  const body = night ? '#4a4a52' : '#eae0d0'
  const roof = night ? '#3a3540' : mix(accent, '#5b4636', 0.55)
  const windows = [
    { x: x + 26, y: y + 34, w: 34, h: 30 },
    { x: x + 74, y: y + 34, w: 34, h: 30 },
    { x: x + 26, y: y + 84, w: 34, h: 30 },
    { x: x + 74, y: y + 84, w: 34, h: 30 },
  ]

  return (
    <g>
      {style === 'study' && (
        <>
          <rect x={x} y={y} width={w} height={h} rx="6" fill={body} />
          <polygon points={`${x - 16},${y} ${x + w / 2},${y - 52} ${x + w + 16},${y}`} fill={roof} />
          {windows.map((win, i) => (
            <Window key={i} {...win} lit={i < litWindows} warm={warm} />
          ))}
          <rect x={x + w - 78} y={y + 62} width={52} height={88} rx="4" fill={accent} opacity="0.8" />
          <rect x={x + w - 70} y={y + 70} width={36} height={30} rx="3" fill={studyGlow > 0 ? warm : '#2f3b46'} opacity={0.4 + studyGlow * 0.6} />
        </>
      )}
      {style === 'white' && (
        <>
          <rect x={x} y={y} width={w} height={h} rx="4" fill={night ? '#5a5a60' : '#fbf7ef'} />
          <rect x={x - 12} y={y - 14} width={w + 24} height={16} rx="4" fill={roof} />
          <rect x={x + 20} y={y + 30} width={72} height={52} rx="4" fill={night ? '#33414d' : '#cfe3ef'} />
          <rect x={x + 108} y={y + 30} width={54} height={40} rx="4" fill={night ? '#33414d' : '#cfe3ef'} />
          <rect x={x + 108} y={y + 82} width={54} height={40} rx="4" fill={night ? '#33414d' : '#cfe3ef'} />
          {litWindows > 0 && <rect x={x + 20} y={y + 30} width={72} height={52} rx="4" fill={warm} opacity={0.6} />}
          {litWindows > 1 && <rect x={x + 108} y={y + 30} width={54} height={40} rx="4" fill={warm} opacity={0.55} />}
          <rect x={x + 20} y={y + 96} width={40} height={54} rx="3" fill={accent} opacity="0.85" />
        </>
      )}
      {style === 'cabin' && (
        <>
          <polygon points={`${x - 14},${baseY} ${x + w / 2},${y - 56} ${x + w + 14},${baseY}`} fill={roof} />
          <rect x={x + 22} y={y + 10} width={w - 44} height={h - 10} rx="4" fill={body} />
          {Array.from({ length: 6 }).map((_, i) => (
            <line key={i} x1={x + 22} y1={y + 24 + i * 20} x2={x + w - 22} y2={y + 24 + i * 20} stroke={mix(body, '#000000', 0.2)} strokeWidth="2" />
          ))}
          <Window x={x + 46} y={y + 44} w={44} h={40} lit={litWindows > 0} warm={warm} />
          <Window x={x + 46} y={y + 100} w={44} h={34} lit={litWindows > 1} warm={warm} />
          <rect x={x + 128} y={y + 74} width={58} height={76} rx="4" fill={accent} opacity="0.85" />
        </>
      )}
      {style === 'stone' && (
        <>
          <rect x={x + 8} y={y + 18} width={w - 16} height={h - 18} rx="8" fill={body} />
          <rect x={x} y={y + 4} width={w} height={20} rx="6" fill={roof} />
          <rect x={x + 40} y={y - 34} width={26} height={44} rx="4" fill={roof} />
          {windows.map((win, i) => (
            <Window key={i} {...win} lit={i < litWindows} warm={warm} />
          ))}
          <path d={`M${x + w - 74} ${baseY} v-52 a26 26 0 0 1 52 0 v52 z`} fill={accent} opacity="0.85" />
        </>
      )}
      {style === 'courtyard' && (
        <>
          <rect x={x - 10} y={y + 40} width={22} height={h - 40} fill={body} />
          <rect x={x + w - 12} y={y + 40} width={22} height={h - 40} fill={body} />
          <rect x={x + 12} y={y} width={w - 24} height={h} rx="4" fill={body} />
          <path d={`M${x - 26} ${y} q${(w + 52) / 2} -40 ${w + 52} 0 z`} fill={roof} />
          <rect x={x + 30} y={y + 42} width={60} height={46} rx="4" fill={night ? '#33414d' : '#cfdbe3'} />
          {litWindows > 0 && <rect x={x + 30} y={y + 42} width={60} height={46} rx="4" fill={warm} opacity="0.62" />}
          <rect x={x + w - 92} y={y + 42} width={60} height={46} rx="4" fill={night ? '#33414d' : '#cfdbe3'} />
          {litWindows > 1 && <rect x={x + w - 92} y={y + 42} width={60} height={46} rx="4" fill={warm} opacity="0.55" />}
          <rect x={x + w / 2 - 22} y={y + 96} width={44} height={54} rx="4" fill={accent} opacity="0.9" />
        </>
      )}
      {style === 'loft' && (
        <>
          <rect x={x + 24} y={y - 44} width={w - 48} height={h + 44} rx="5" fill={body} />
          <rect x={x + 16} y={y - 56} width={w - 32} height={16} rx="4" fill={roof} />
          {windows.map((win, i) => (
            <Window key={i} {...win} lit={i < litWindows} warm={warm} />
          ))}
          <rect x={x + w - 78} y={y - 30} width={34} height={26} rx="3" fill={night ? '#33414d' : '#cfdbe3'} />
          <rect x={x + w - 78} y={y + 108} width={44} height={42} rx="3" fill={accent} opacity="0.85" />
        </>
      )}
    </g>
  )
}

type InteriorProps = {
  scene: Scene
  time: TimeOfDay
  memorial: boolean
  photos: Photo[]
  stories: Story[]
  letters: Letter[]
  departed: Departed[]
}

/** 内景：客厅剖面。相框墙与书架随内容增长（设计 4.4）。 */
export function InteriorView({ scene, time, memorial, photos, stories, letters, departed }: InteriorProps) {
  const c = palette(scene, time)
  const wall = memorial ? mix('#e9e2d6', '#8d8378', 0.25) : mix(scene.sky[1], '#ffffff', 0.55)
  const floor = memorial ? '#6f5a45' : mix(scene.accent, '#8a6a45', 0.6)
  const slots = 9
  const booksPerShelf = 9
  const shelves = 3
  const totalBookSlots = booksPerShelf * shelves
  const books = Math.min(stories.length * 3, totalBookSlots)
  const lampOn = time === 'night' || time === 'dusk'

  return (
    <svg className="scene" viewBox="0 0 1000 560" role="img" aria-label="客厅内景">
      <defs>
        <linearGradient id="sky2" x1="0" y1="0" x2="0" y2="1">
          <stop offset="0%" stopColor={c.sky0} />
          <stop offset="100%" stopColor={c.sky1} />
        </linearGradient>
        <radialGradient id="lamp2">
          <stop offset="0%" stopColor="#ffd79a" stopOpacity="0.85" />
          <stop offset="100%" stopColor="#ffd79a" stopOpacity="0" />
        </radialGradient>
      </defs>

      <rect width="1000" height="560" fill={wall} />
      <rect y="430" width="1000" height="130" fill={floor} />
      {Array.from({ length: 10 }).map((_, i) => (
        <line key={i} x1={i * 100} y1="430" x2={i * 100} y2="560" stroke={mix(floor, '#000000', 0.18)} strokeWidth="2" opacity="0.5" />
      ))}

      {/* 窗：窗外就是这座居所所在的地方 */}
      <g>
        <rect x="742" y="108" width="206" height="176" rx="10" fill="#ffffff" opacity="0.55" />
        <rect x="754" y="120" width="182" height="152" rx="6" fill="url(#sky2)" />
        <circle cx="828" cy="166" r="18" fill={time === 'night' ? '#f4f1e4' : '#ffe0a8'} />
        <polygon points="754,272 830,206 906,272" fill={c.far} opacity="0.85" />
        <polygon points="836,272 900,222 936,272" fill={c.mid} opacity="0.85" />
        <line x1="845" y1="120" x2="845" y2="272" stroke="#ffffff" strokeWidth="5" opacity="0.85" />
        <line x1="754" y1="196" x2="936" y2="196" stroke="#ffffff" strokeWidth="5" opacity="0.85" />
      </g>

      {/* 相框墙 */}
      <PhotoWall photos={photos} capacity={slots} memorial={memorial} />

      {/* 书架 */}
      <Shelf books={books} shelves={shelves} perShelf={booksPerShelf} memorial={memorial} />

      {/* 桌子与灯 */}
      <g>
        <rect x="330" y="392" width="286" height="14" rx="5" fill={mix(floor, '#000000', 0.25)} />
        <rect x="348" y="406" width="10" height="46" fill={mix(floor, '#000000', 0.3)} />
        <rect x="588" y="406" width="10" height="46" fill={mix(floor, '#000000', 0.3)} />
        {lampOn && <circle cx="470" cy="356" r="92" fill="url(#lamp2)" />}
        <rect x="462" y="330" width="18" height="62" rx="6" fill={lampOn ? '#ffd58a' : '#c9c2b4'} />
        <rect x="452" y="384" width="38" height="10" rx="4" fill="#8a6a45" />
        <rect x="396" y="368" width="52" height="24" rx="3" fill={memorial ? '#b9a98f' : '#f3ead9'} stroke="#c8ab84" />
      </g>

      {/* 信箱 */}
      <g transform="translate(96 372)">
        <rect x="0" y="0" width="74" height="52" rx="8" fill={memorial ? '#8a7a63' : '#c8873f'} opacity="0.9" />
        <rect x="8" y="10" width="58" height="8" rx="4" fill="#fff6e6" opacity="0.8" />
        <circle cx="58" cy="36" r="14" fill="#4a3826" />
        <text x="58" y="42" textAnchor="middle" fontSize="14" fill="#ffe9c2" fontWeight="700">
          {letters.length}
        </text>
      </g>

      {memorial && (
        <g transform="translate(860 372)">
          <rect x="0" y="10" width="96" height="10" rx="4" fill="#6f5a45" />
          <rect x="14" y="20" width="8" height="34" fill="#6f5a45" />
          <rect x="74" y="20" width="8" height="34" fill="#6f5a45" />
          <circle className="flame" cx="48" cy="2" r="9" fill="#ffcf85" />
          <rect x="44" y="6" width="8" height="16" rx="3" fill="#f0e2c6" />
          {departed.slice(0, 1).map((person) => (
            <text key={person.id} x="48" y="-14" textAnchor="middle" fontSize="13" fill="#6b5a45">
              {person.name}
            </text>
          ))}
        </g>
      )}
    </svg>
  )
}

function PhotoWall({ photos, capacity, memorial }: { photos: Photo[]; capacity: number; memorial: boolean }) {
  const tints = ['#d9b48f', '#b7c4a8', '#c9a9a6', '#a8bcc9', '#d3c39a', '#bda9c4', '#c4b58f', '#9fc0b5', '#cbb193']
  return (
    <g transform="translate(96 92)">
      {Array.from({ length: capacity }).map((_, i) => {
        const column = i % 3
        const row = Math.floor(i / 3)
        const x = column * 128
        const y = row * 96
        const photo = photos[i]
        if (!photo) {
          return (
            <g key={i}>
              <rect x={x} y={y} width={104} height={80} rx="5" fill="none" stroke={memorial ? '#a99b8a' : '#c8ab84'} strokeDasharray="6 5" strokeWidth="2" opacity="0.75" />
              <text x={x + 52} y={y + 46} textAnchor="middle" fontSize="11" fill="#a2947f">
                待放
              </text>
            </g>
          )
        }
        return (
          <g key={photo.id} className="frame-pop">
            <rect x={x - 4} y={y - 4} width={112} height={88} rx="6" fill="#fdf8ee" stroke="#b99a72" strokeWidth="2" />
            <rect x={x} y={y} width={104} height={62} rx="4" fill={tints[i % tints.length]} />
            <circle cx={x + 30} cy={y + 24} r="11" fill="#ffffff" opacity="0.45" />
            <polygon points={`${x},${y + 62} ${x + 34},${y + 30} ${x + 68},${y + 62}`} fill="#ffffff" opacity="0.35" />
            <text x={x + 6} y={y + 76} fontSize="11" fill="#6b5a45">
              {photo.year}
            </text>
          </g>
        )
      })}
    </g>
  )
}

function Shelf({ books, shelves, perShelf, memorial }: { books: number; shelves: number; perShelf: number; memorial: boolean }) {
  const wood = memorial ? '#6b5a45' : '#8a6a45'
  let placed = 0
  return (
    <g transform="translate(672 330)">
      {Array.from({ length: shelves }).map((_, row) => {
        const y = row * 52
        const cells = Array.from({ length: perShelf }).map(() => {
          const filled = placed < books
          placed += 1
          return filled
        })
        return (
          <g key={row}>
            <rect x="-8" y={y + 40} width={perShelf * 18 + 20} height="7" rx="3" fill={wood} />
            {cells.map((filled, i) => (
              <rect
                key={i}
                x={i * 18}
                y={y + (i % 3 === 0 ? 8 : i % 3 === 1 ? 14 : 10)}
                width="13"
                height={30 - (i % 3) * 4}
                rx="2"
                fill={filled ? ['#c8873f', '#7f9c86', '#b8624a', '#8b7ab0', '#c4a35a'][i % 5] : '#d8d2c6'}
                opacity={filled ? 1 : 0.5}
              />
            ))}
          </g>
        )
      })}
    </g>
  )
}
