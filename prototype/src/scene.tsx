import {
  ALBUM,
  HOUSEHOLD,
  WALL_CAPACITY,
  type Departed,
  type Letter,
  type Member,
  type Photo,
  type Scene,
  type Story,
  type TimeOfDay,
} from './data'

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

const TIME_WORD: Record<TimeOfDay, string> = { dawn: '清晨', dusk: '黄昏', night: '夜晚' }

/**
 * 屋外：真实照片 + 时段调色 + 亮灯的窗。
 * 内容驱动（设计 4.4）：家人越多，亮着的窗越多；切到"之后"，门口亮一盏灯，故人处一列长明灯。
 */
export function SceneView(props: SceneProps) {
  const { scene, time, memorial, members, departed, photos, letters } = props
  const lit = memorial ? scene.lights.length : Math.min(members.length + 1, scene.lights.length)
  const family = members.length + 1

  return (
    <div
      className={`scene scene-photo tone-${time} ${memorial ? 'scene-memorial' : ''}`}
      role="img"
      aria-label={`${scene.name}外景，${TIME_WORD[time]}${memorial ? '，之后' : ''}`}
    >
      <div className="scene-frame">
        <div className="scene-world">
          <img className="scene-base" src={scene.photo} alt="" draggable={false} />
          <div className="scene-grade" aria-hidden />

          <div className="scene-lights" aria-hidden>
            {scene.lights.map((spot, i) => (
              <span
                key={i}
                className={`scene-light ${i < lit ? 'on' : ''}`}
                style={{
                  left: `${spot.x}%`,
                  top: `${spot.y}%`,
                  width: `${spot.w}%`,
                  height: `${spot.h}%`,
                  animationDelay: `${i * 0.35}s`,
                  ['--flicker' as string]: `${3.6 + (i % 3) * 0.9}s`,
                }}
              />
            ))}
          </div>

          {memorial && (
            <div className="scene-memorial-marks" aria-hidden>
              <span
                className="scene-lamp"
                style={{ left: `${scene.lamp.x}%`, top: `${scene.lamp.y}%`, width: `${scene.lamp.w}%`, height: `${scene.lamp.h}%` }}
              />
              {departed.map((person, i) => (
                <span
                  key={person.id}
                  className="scene-candle"
                  title={person.name}
                  style={{ left: `${scene.candle.x + i * 4.4}%`, top: `${scene.candle.y}%` }}
                />
              ))}
            </div>
          )}
        </div>

        <div className={`scene-air air-${scene.air}`} aria-hidden />
        <div className="scene-vignette" aria-hidden />
        <div className="scene-grain" aria-hidden />
      </div>

      <div className="scene-plate">
        <b>{HOUSEHOLD}</b>
        <i>·</i>
        <span>{scene.name}</span>
        <em>
          家人 {family} · 照片 {photos.length} · 留言 {letters.length}
        </em>
      </div>
    </div>
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

const SHELVES = 3
const PER_SHELF = 9

/** 屋里：真实室内照片做氛围，前景是这家人自己的东西——相册墙与书架随内容增长（设计 4.4）。 */
export function InteriorView({ scene, time, memorial, photos, stories, letters, departed }: InteriorProps) {
  const lampOn = time !== 'dawn'
  const filled = Math.min(stories.length * 3, SHELVES * PER_SHELF)
  return (
    <div
      className={`scene scene-inside tone-${time} ${memorial ? 'scene-memorial' : ''}`}
      role="img"
      aria-label="屋里：相册墙与书架"
    >
      <div className="scene-frame">
        <img className="scene-base scene-base-soft" src={scene.interior} alt="" draggable={false} />
        <div className="scene-grade" aria-hidden />
        {lampOn && <div className="inside-lamp" aria-hidden />}

        <div className="inside-things">
          <section className="inside-wall">
            <header>
              <h4>相册墙</h4>
              <span>
                {photos.length} / {WALL_CAPACITY}
              </span>
            </header>
            <div className="frames">
              {Array.from({ length: WALL_CAPACITY }).map((_, i) => {
                const photo = photos[i]
                if (!photo) {
                  const firstEmpty = i === photos.length
                  return (
                    <div className="frame is-empty" key={`empty-${i}`}>
                      {firstEmpty && <span>待放</span>}
                    </div>
                  )
                }
                return (
                  <figure className="frame" key={photo.id} title={photo.caption}>
                    <img src={photo.img} alt={photo.caption} />
                    <figcaption>{photo.year}</figcaption>
                  </figure>
                )
              })}
            </div>
          </section>

          <aside className="inside-shelf">
            <header>
              <h4>书房</h4>
              <span>
                {stories.length} 段讲述
              </span>
            </header>
            <div className="shelf">
              {Array.from({ length: SHELVES }).map((_, row) => (
                <div className="shelf-row" key={row}>
                  {Array.from({ length: PER_SHELF }).map((_, col) => {
                    const index = row * PER_SHELF + col
                    return <span key={col} className={`book ${index < filled ? 'on' : ''}`} />
                  })}
                </div>
              ))}
            </div>
            <div className="inside-mail">
              <svg viewBox="0 0 24 24" aria-hidden>
                <path
                  d="M3 7.5A2.5 2.5 0 0 1 5.5 5h13A2.5 2.5 0 0 1 21 7.5v9A2.5 2.5 0 0 1 18.5 19h-13A2.5 2.5 0 0 1 3 16.5v-9Zm2 .5 7 5 7-5"
                  fill="none"
                  stroke="currentColor"
                  strokeWidth="1.6"
                  strokeLinejoin="round"
                  strokeLinecap="round"
                />
              </svg>
              <span>信箱里有 {letters.length} 封，到日子才打开</span>
            </div>
          </aside>
        </div>

        {memorial && (
          <div className="inside-candles" aria-hidden>
            {departed.map((person) => (
              <span key={person.id} className="inside-candle" title={person.name} />
            ))}
          </div>
        )}

        <div className="scene-vignette" aria-hidden />
        <div className="scene-grain" aria-hidden />
      </div>
    </div>
  )
}

export const SCENE_ALBUM = ALBUM
