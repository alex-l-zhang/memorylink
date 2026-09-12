import { useEffect, useState, type ReactNode } from 'react'
import {
  ALBUM,
  HOUSEHOLD,
  NAV,
  OWNER,
  SCENES,
  SHELF_CAPACITY,
  WALL_CAPACITY,
  initialState,
  type ExecutorStage,
  type Letter,
  type NavId,
  type State,
  type TimeOfDay,
} from './data'
import { InteriorView, SceneView } from './scene'

const TIME_LABEL: Record<TimeOfDay, string> = { dawn: '清晨', dusk: '黄昏', night: '夜晚' }
const STAGE_LABEL: Record<ExecutorStage, string> = {
  none: '未指定',
  requested: '已申请开启',
  cooling: '冷静期中（第 3 天 / 共 7 天）',
  active: '已开启',
}

type Toast = { id: number; text: string; sub?: string }

export default function App() {
  const [state, setState] = useState<State>(initialState)
  const [nav, setNav] = useState<NavId>('dwelling')
  const [inside, setInside] = useState(false)
  const [pickerOpen, setPickerOpen] = useState(false)
  const [porch, setPorch] = useState(false)
  const [toasts, setToasts] = useState<Toast[]>([])

  const scene = SCENES.find((s) => s.id === state.sceneId) ?? SCENES[0]
  const memorial = state.mode === 'memorial'
  usePrototypeTitle(state.mode, state.time, scene.name)

  function toast(text: string, sub?: string) {
    const id = Date.now() + Math.random()
    setToasts((prev) => [...prev, { id, text, sub }])
    window.setTimeout(() => setToasts((prev) => prev.filter((t) => t.id !== id)), 2800)
  }

  function toggleMode() {
    setState((prev) => {
      const next = prev.mode === 'living' ? 'memorial' : 'living'
      return {
        ...prev,
        mode: next,
        time: next === 'memorial' ? 'dusk' : 'dawn',
        executors: prev.executors.map((e) =>
          next === 'memorial' && e.stage === 'none' ? { ...e, stage: 'active' } : e,
        ),
      }
    })
    if (!memorial) {
      toast('房子安静下来了', '门口亮起一盏灯。这一刻由家人开启，系统不会自己判断。')
      setInside(false)
    } else {
      toast('回到现在', '你还在，屋子照常。')
    }
  }

  function addPhoto() {
    const years = ['1962', '1974', '1980', '1993', '2008', '2015']
    const captions = ['在河边拍的那张', '我第一次上讲台', '院子里的夏天', '一家人在门口', '老屋拆之前']
    const i = state.photos.length
    if (state.photos.length >= WALL_CAPACITY) {
      toast('相册墙满了', '剩下的照片会收进抽屉，不会丢。')
      return
    }
    setState((prev) => ({
      ...prev,
      photos: [
        ...prev.photos,
        { id: `p${Date.now()}`, year: years[i % years.length], caption: captions[i % captions.length], img: ALBUM[i % ALBUM.length] },
      ],
    }))
    toast('客厅的墙上，多了一张照片', '父亲那一辈的样子，又多留住一点。')
  }

  function addStory() {
    const titles = ['那年冬天，我一个人走夜路', '你奶奶第一次来我们家', '我教的第一届学生', '灶台上的那口锅']
    const texts = [
      '雪下得很大，我以为自己走不到家。后来是你爷爷提着灯在村口等我。',
      '她穿了一件蓝布褂子，进门先帮我娘挑水，一句话没说。',
      '有个孩子每天走十里路来上课，后来他当了医生。',
      '那口锅煮过一家八口人的饭，锅底都磨亮了。',
    ]
    const i = state.stories.length
    setState((prev) => ({
      ...prev,
      stories: [
        ...prev.stories,
        { id: `s${Date.now()}`, title: titles[i % titles.length], by: OWNER, text: texts[i % texts.length] },
      ],
    }))
    toast('书架上，多了一段讲述', i + 1 >= SHELF_CAPACITY ? '书架快满了——这些话以后都在这儿。' : '这些话以后都在这儿。')
  }

  function addLetter(letter: Omit<Letter, 'id' | 'declined'>) {
    setState((prev) => ({ ...prev, letters: [...prev.letters, { ...letter, id: `l${Date.now()}`, declined: false }] }))
    toast('信已经放进信箱', `到那天，${letter.to} 会收到。之前谁也看不到。`)
  }

  function addMember() {
    const pool = [
      { name: '陈亮', relation: '侄子', room: '西厢房' },
      { name: '李慧', relation: '儿媳', room: '二楼西间' },
      { name: '陈舟', relation: '孙子', room: '靠窗的小间' },
    ]
    if (state.members.length >= 4) {
      toast('楼上楼下都住满了', '再来的人，会先住在门房，等你安排。')
      return
    }
    const pick = pool[state.members.length % pool.length]
    setState((prev) => ({ ...prev, members: [...prev.members, { id: `m${Date.now()}`, private: true, ...pick }] }))
    toast('二楼多了一扇亮着灯的窗', `${pick.name}住进了${pick.room}。`)
  }

  function addDeparted() {
    const pool = [
      { name: '陈广福', relation: '父亲' },
      { name: '周桂英', relation: '母亲' },
      { name: '林素芬', relation: '姐姐' },
    ]
    const pick = pool[state.departed.length % pool.length]
    setState((prev) => ({
      ...prev,
      departed: [...prev.departed, { id: `d${Date.now()}`, ...pick }],
    }))
    toast('院子里多种了一棵树', `这棵树替${pick.relation}${pick.name}站着。`)
  }

  function advanceExecutor(id: string) {
    setState((prev) => ({
      ...prev,
      executors: prev.executors.map((e) => {
        if (e.id !== id) return e
        const next: ExecutorStage = e.stage === 'none' ? 'requested' : e.stage === 'requested' ? 'cooling' : 'active'
        return { ...e, stage: next }
      }),
    }))
  }

  function answerAsk() {
    const ask = state.pendingAsk
    if (!ask) return
    setState((prev) => ({
      ...prev,
      stories: [
        ...prev.stories,
        {
          id: `s${Date.now()}`,
          title: ask.question,
          by: OWNER,
          text: '过年啊，杀一口猪，蒸一大笼馒头。你太爷爷会把最好的一块肉留到年三十，说那叫"压桌"。',
          askedBy: ask.by,
        },
      ],
      pendingAsk: null,
    }))
    toast('念念的问题，变成了一段讲述', '它已经放进书房了。')
  }

  return (
    <div className={`app ${memorial ? 'is-memorial' : ''}`}>
      <TopBar
        sceneName={scene.name}
        place={scene.place}
        kind={scene.kind}
        mode={state.mode}
        time={state.time}
        onMode={toggleMode}
        onTime={(t) => setState((prev) => ({ ...prev, time: t }))}
        onPick={() => setPickerOpen(true)}
        onPorch={() => setPorch(true)}
      />

      <div className="layout">
        <nav className="nav">
          <div className="nav-house">
            <span className="nav-house-name">{HOUSEHOLD}</span>
            <span className="nav-house-sub">{scene.name}</span>
          </div>
          {NAV.map((item) => (
            <button
              key={item.id}
              className={`nav-item ${nav === item.id ? 'active' : ''}`}
              onClick={() => setNav(item.id)}
            >
              {item.label}
              {item.id === 'letters' && state.letters.length > 0 && <em>{state.letters.length}</em>}
              {item.id === 'cowrite' && state.pendingAsk && <em className="hot">1</em>}
            </button>
          ))}
        </nav>

        <main className="main">
          {nav === 'dwelling' && (
            <Dwelling
              state={state}
              scene={scene}
              inside={inside}
              onInside={setInside}
              onAddPhoto={addPhoto}
              onAddStory={addStory}
              onAddMember={addMember}
              onAddDeparted={addDeparted}
            />
          )}
          {nav === 'rooms' && <Rooms state={state} onAddMember={addMember} />}
          {nav === 'letters' && <Letters state={state} onAdd={addLetter} />}
          {nav === 'keys' && <Keys state={state} onAdvance={advanceExecutor} />}
          {nav === 'cowrite' && <CoWrite state={state} onAnswer={answerAsk} />}
          {nav === 'bounds' && <Bounds />}
        </main>
      </div>

      {state.mode === 'living' && nav === 'dwelling' && (
        <div className="putbar">
          <span className="putbar-label">今天，往屋里放一件</span>
          <button className="put" onClick={addPhoto}>
            <b>一张照片</b>
            <i>客厅墙上会多一个相框</i>
          </button>
          <button className="put" onClick={addStory}>
            <b>一段讲述</b>
            <i>书架上会多一本书</i>
          </button>
          <button
            className="put"
            onClick={() =>
              addLetter({ to: '陈念', openAt: '2030-09-01（念念 21 岁）', text: '念念，爷爷想说的话都在这儿了。' })
            }
          >
            <b>一句留给某人的话</b>
            <i>先锁进信箱，到那天才打开</i>
          </button>
        </div>
      )}

      {pickerOpen && (
        <Picker
          current={state.sceneId}
          onClose={() => setPickerOpen(false)}
          onPick={(id) => {
            setState((prev) => ({ ...prev, sceneId: id }))
            setPickerOpen(false)
            const picked = SCENES.find((s) => s.id === id)
            toast('一家人搬到了新地方', picked ? `${picked.place}·${picked.kind}——位置是全家一起定的。` : undefined)
          }}
        />
      )}

      {porch && <Porch onClose={() => setPorch(false)} scene={scene} state={state} />}

      <div className="toast-stack">
        {toasts.map((t) => (
          <div className="toast" key={t.id}>
            <strong>{t.text}</strong>
            {t.sub && <span>{t.sub}</span>}
          </div>
        ))}
      </div>
    </div>
  )
}

function TopBar(props: {
  sceneName: string
  place: string
  kind: string
  mode: State['mode']
  time: TimeOfDay
  onMode: () => void
  onTime: (t: TimeOfDay) => void
  onPick: () => void
  onPorch: () => void
}) {
  const memorial = props.mode === 'memorial'
  return (
    <header className="topbar">
      <div className="brand">
        <span className="brand-mark">忆联</span>
        <span className="brand-sub">
          {props.place} · {props.kind}
        </span>
      </div>

      <div className="controls">
        <div className="seg">
          <button className={!memorial ? 'on' : ''} onClick={() => memorial && props.onMode()}>
            现在
          </button>
          <button className={memorial ? 'on' : ''} onClick={() => !memorial && props.onMode()}>
            之后
          </button>
        </div>
        <div className="seg soft">
          {(['dawn', 'dusk', 'night'] as TimeOfDay[]).map((t) => (
            <button key={t} className={props.time === t ? 'on' : ''} onClick={() => props.onTime(t)}>
              {TIME_LABEL[t]}
            </button>
          ))}
        </div>
        <button className="ghost" onClick={props.onPick}>
          换个地方
        </button>
        <button className="ghost" onClick={props.onPorch}>
          门口看一眼
        </button>
      </div>
    </header>
  )
}

function Dwelling(props: {
  state: State
  scene: (typeof SCENES)[number]
  inside: boolean
  onInside: (v: boolean) => void
  onAddPhoto: () => void
  onAddStory: () => void
  onAddMember: () => void
  onAddDeparted: () => void
}) {
  const { state, scene, inside } = props
  const memorial = state.mode === 'memorial'
  return (
    <div className="view">
      <div className="stage">
        {inside ? (
          <InteriorView
            scene={scene}
            time={state.time}
            memorial={memorial}
            photos={state.photos}
            stories={state.stories}
            letters={state.letters}
            departed={state.departed}
          />
        ) : (
          <SceneView
            scene={scene}
            time={state.time}
            memorial={memorial}
            members={state.members}
            photos={state.photos}
            stories={state.stories}
            letters={state.letters}
            departed={state.departed}
          />
        )}
        <div className="stage-switch">
          <button className={!inside ? 'on' : ''} onClick={() => props.onInside(false)}>
            屋外
          </button>
          <button className={inside ? 'on' : ''} onClick={() => props.onInside(true)}>
            屋里
          </button>
        </div>
        <div className="stage-caption">
          <strong>{scene.name}</strong>
          <span>{scene.note}</span>
        </div>
      </div>

      <div className="grid">
        <Card title="这间屋子现在有什么">
          <ul className="stat">
            <li>
              <b>{state.photos.length}</b>
              <span>张照片在客厅墙上</span>
            </li>
            <li>
              <b>{state.stories.length}</b>
              <span>段讲述在书房</span>
            </li>
            <li>
              <b>{state.letters.length}</b>
              <span>封信锁在信箱</span>
            </li>
            <li>
              <b>{state.departed.length}</b>
              <span>棵树在院子里</span>
            </li>
          </ul>
          <p className="hint">屋子跟着内容长。你每放一件东西，它都不一样——这不是一个填一次就结束的表格。</p>
        </Card>

        <Card title="屋子的两次样子">
          <div className="compare">
            <div>
              <h4>现在</h4>
              <p>我住着。整理照片、讲从前的事、把说不出口的话先存起来。子女随时可以来听。</p>
            </div>
            <div className={memorial ? 'lit' : ''}>
              <h4>之后</h4>
              <p>门由家人打开。屋子里的东西还在，讲述还在，信到了日子会自己送到。</p>
            </div>
          </div>
          <p className="hint">
            {memorial
              ? '当前是"之后"的样子：门口亮了一盏灯，客厅多了一张照片的位置，信在等日子。'
              : '切到右上角的"之后"，看看同一间屋子的另一副样子。'}
          </p>
        </Card>

        {state.mode === 'living' && (
          <Card title="原型演示台" note="用于演示设计机制，真机上会换成家人邀请与内容导入">
            <div className="demo-row">
              <button className="btn" onClick={props.onAddMember}>
                家里多一个人
              </button>
              <button className="btn" onClick={props.onAddDeparted}>
                院子里多种一棵树
              </button>
              <button className="btn" onClick={props.onAddPhoto}>
                墙上多一张照片
              </button>
              <button className="btn" onClick={props.onAddStory}>
                书架多一段讲述
              </button>
            </div>
          </Card>
        )}
      </div>
    </div>
  )
}

function Rooms({ state, onAddMember }: { state: State; onAddMember: () => void }) {
  return (
    <div className="view">
      <Card title="这户人家住着谁" note="房间属于个人；屋主是这间房子的管理者，不是所有房间的查看者">
        <div className="rooms">
          <div className="room self">
            <b>{OWNER}</b>
            <span>我 · 书房</span>
            <em>我自己</em>
          </div>
          {state.members.map((m) => (
            <div className="room" key={m.id}>
              <b>{m.name}</b>
              <span>
                {m.relation} · {m.room}
              </span>
              <em className={m.private ? 'private' : ''}>{m.private ? '只给本人' : '家里都能进'}</em>
            </div>
          ))}
          {state.departed.map((d) => (
            <div className="room gone" key={d.id}>
              <b>{d.name}</b>
              <span>{d.relation} · 堂屋</span>
              <em>留着他的位置</em>
            </div>
          ))}
        </div>
        <button className="btn" onClick={onAddMember}>
          给家里人多留一间房
        </button>
        <p className="hint">
          房间可以先留着，人还没进来。房间在屋子内部，不会跑到家族关系图上——这是它跟"占位节点"最不一样的地方。
        </p>
      </Card>
    </div>
  )
}

function Letters({ state, onAdd }: { state: State; onAdd: (l: Omit<Letter, 'id' | 'declined'>) => void }) {
  const [to, setTo] = useState('陈念')
  const [openAt, setOpenAt] = useState('2030-09-01')
  const [text, setText] = useState('')
  return (
    <div className="view">
      <Card title="留给某人的话" note="现在写，到那天才打开——中间谁也看不到">
        <div className="letters">
          {state.letters.map((l) => (
            <div className="letter" key={l.id}>
              <div className="letter-head">
                <span className="seal">封</span>
                <div>
                  <b>给 {l.to}</b>
                  <span>开启时间：{l.openAt}</span>
                </div>
              </div>
              <p>{l.text}</p>
              <div className="letter-foot">
                <label className="switch">
                  <input type="checkbox" defaultChecked={l.declined} />
                  <span>收信人可以选"暂不接收"</span>
                </label>
              </div>
            </div>
          ))}
        </div>
      </Card>

      <Card title="再写一封">
        <div className="form">
          <label>
            <span>写给谁</span>
            <select value={to} onChange={(e) => setTo(e.target.value)}>
              {['陈念', '陈小雨', '陈松', '王秀兰'].map((n) => (
                <option key={n}>{n}</option>
              ))}
            </select>
          </label>
          <label>
            <span>什么时候打开</span>
            <input value={openAt} onChange={(e) => setOpenAt(e.target.value)} />
          </label>
          <label className="wide">
            <span>想说的话</span>
            <textarea rows={3} value={text} onChange={(e) => setText(e.target.value)} placeholder="不用长，一句也行。" />
          </label>
          <button
            className="btn primary"
            onClick={() => {
              onAdd({ to, openAt, text: text.trim() || '（还没写内容）' })
              setText('')
            }}
          >
            放进信箱
          </button>
        </div>
        <p className="hint">
          这是别的地方给不了的一件事：有些话，现在不能让活着的人看到。所以要有一个"到某一天再拆"的地方。
        </p>
      </Card>
    </div>
  )
}

function Keys({ state, onAdvance }: { state: State; onAdvance: (id: string) => void }) {
  const stages: ExecutorStage[] = ['none', 'requested', 'cooling', 'active']
  return (
    <div className="view">
      <Card title="钥匙交给谁" note="系统不会自己判断谁走了。只有你指定的人来开门，门才会开">
        {state.executors.map((e) => (
          <div className="exec" key={e.id}>
            <div>
              <b>{e.name}</b>
              <span>{e.relation}</span>
            </div>
            <em className={`stage stage-${e.stage}`}>{STAGE_LABEL[e.stage]}</em>
            <button className="btn" onClick={() => onAdvance(e.id)}>
              {e.stage === 'none' ? '请他做开门人' : e.stage === 'active' ? '已完成' : '推进一步（演示）'}
            </button>
          </div>
        ))}
        <ol className="flow">
          <li className={stages.indexOf(state.executors[0]?.stage ?? 'none') >= 0 ? 'done' : ''}>你当面指定开门人（可以随时改）</li>
          <li className={stages.indexOf(state.executors[0]?.stage ?? 'none') >= 1 ? 'done' : ''}>开门人提交开启申请，需要二次确认</li>
          <li className={stages.indexOf(state.executors[0]?.stage ?? 'none') >= 2 ? 'done' : ''}>7 天冷静期：这段时间里，你或任何家人都能撤回</li>
          <li className={stages.indexOf(state.executors[0]?.stage ?? 'none') >= 3 ? 'done' : ''}>屋子进入纪念状态，信按你定的日子送出</li>
        </ol>
        <p className="hint">
          为什么不做"自动判断"：一旦判错，人还活着，屋子却已经变成纪念的样子——这种错误没有补救。所以这件事只能由人来做。
        </p>
      </Card>
    </div>
  )
}

function CoWrite({ state, onAnswer }: { state: State; onAnswer: () => void }) {
  return (
    <div className="view">
      <Card title="一起写" note="入口不是「准备身后事」，是「家里有人问你」">
        {state.pendingAsk ? (
          <div className="ask">
            <div className="ask-head">
              <b>{state.pendingAsk.by}</b>
              <span>问爷爷</span>
            </div>
            <p className="ask-q">{state.pendingAsk.question}</p>
            <button className="btn primary" onClick={onAnswer}>
              爷爷回答他
            </button>
            <p className="hint">回答完，这段讲述会自己走进书房——不用另外再录一遍。</p>
          </div>
        ) : (
          <p className="hint">今天没有新的问题。念念上次问的，已经变成一段讲述了。</p>
        )}
      </Card>

      <Card title="书房里现在有什么">
        {state.stories.map((s) => (
          <div className="story" key={s.id}>
            <b>{s.title}</b>
            <p>{s.text}</p>
            <span>
              {s.by}
              {s.askedBy ? ` · 应${s.askedBy}所问` : ''}
            </span>
          </div>
        ))}
      </Card>
    </div>
  )
}

function Bounds() {
  return (
    <div className="view">
      <Card title="谁能看到什么" note="四层，从门廊到一张纸">
        <div className="layers">
          <div className="layer l0">
            <b>门廊</b>
            <span>没登录的人站在门外：看得到门牌上写着"陈家"、有几位家人、几张照片。仅此而已。</span>
          </div>
          <div className="layer l1">
            <b>家族居所</b>
            <span>家里人进得来：看得到屋子的样子、客厅里的共同回忆。</span>
          </div>
          <div className="layer l2">
            <b>个人房间</b>
            <span>每个人的房间只有本人和被他邀请的人能进。屋主也不行。</span>
          </div>
          <div className="layer l3">
            <b>指定内容</b>
            <span>一封信、一张照片，可以只给一个人，只在某个日子。</span>
          </div>
        </div>
      </Card>

      <Card title="这个产品不做的事">
        <ul className="redlines">
          <li>不做 3D 可以走进去的房间——老人要的是"这是我的地方"，不是操作一间屋子</li>
          <li>不做位置买卖、稀缺、涨价</li>
          <li>不做系统自动判定谁走了</li>
          <li>不把家人的名字和照片做成公开可搜索的页面</li>
          <li>不做声音克隆、影像复活</li>
          <li>不在对方不知情的时候把遗属内容推给他</li>
          <li>不用"遗言""墓地"这样的词</li>
          <li>对外不说"给你一座虚拟豪宅"；说的还是"留住家里人的声音和照片"</li>
        </ul>
      </Card>

      <Card title="这份原型对应设计文档的哪几条">
        <ul className="check">
          <li>✓ 位置属于家族、房间属于个人（4.1）</li>
          <li>✓ 插画场景而非 3D，一家一个成套场景（4.2 / 4.3）</li>
          <li>✓ 居所随内容变化：相框、书架、亮灯的窗、院里的树（4.4）</li>
          <li>✓ 三代共写：念念提问 → 爷爷回答 → 进书房（第三章）</li>
          <li>✓ 按时赴约：信锁定开启时间（5.1）</li>
          <li>✓ 选择性公开：信默认无人可见，收信人可"暂不接收"（5.2）</li>
          <li>✓ 低门槛：一句话、一张照片、一段讲述都算数（5.3）</li>
          <li>✓ 门廊：未登录访客只看得到门牌（第七章）</li>
          <li>✓ 开门人 + 7 天冷静期，系统不自动判定（第八章 / 设计红线 3）</li>
        </ul>
      </Card>
    </div>
  )
}

function Card({ title, note, children }: { title: string; note?: string; children: ReactNode }) {
  return (
    <section className="card">
      <header>
        <h3>{title}</h3>
        {note && <p>{note}</p>}
      </header>
      {children}
    </section>
  )
}

function Picker({ current, onPick, onClose }: { current: string; onPick: (id: string) => void; onClose: () => void }) {
  return (
    <div className="overlay" onClick={onClose}>
      <div className="sheet" onClick={(e) => e.stopPropagation()}>
        <h3>一家人想住在哪儿</h3>
        <p className="hint">位置是全家一起定的，房间才是各人自己的。每个地方都是一个完整的样子，不拼装。</p>
        <div className="picker">
          {SCENES.map((s) => (
            <button
              key={s.id}
              className={`pick ${current === s.id ? 'on' : ''}`}
              onClick={() => onPick(s.id)}
            >
              <span className="pick-photo" style={{ backgroundImage: `url(${s.photo})` }} />
              <span className="pick-title">{s.name}</span>
              <span className="pick-note">{s.note}</span>
            </button>
          ))}
        </div>
      </div>
    </div>
  )
}

function Porch({ onClose, scene, state }: { onClose: () => void; scene: (typeof SCENES)[number]; state: State }) {
  return (
    <div className="overlay porch" onClick={onClose}>
      <div className="porch-card" onClick={(e) => e.stopPropagation()}>
        <div className="porch-scene">
          <SceneView
            scene={scene}
            time="night"
            memorial={false}
            members={state.members}
            photos={state.photos}
            stories={state.stories}
            letters={state.letters}
            departed={state.departed}
          />
        </div>
        <div className="porch-body">
          <span className="porch-tag">没登录的时候，看到的就是这些</span>
          <h3>陈家 · {scene.name}</h3>
          <p className="porch-line">
            家人 {state.members.length + 1} 位 · 照片 {state.photos.length} 张 · 最早的一张 1953 年
          </p>
          <p className="hint">屋里透出来的光是真的，里面的东西一件都看不到。</p>
          <div className="porch-actions">
            <button className="btn primary" onClick={onClose}>
              进去（我是家里人）
            </button>
            <button className="btn" onClick={onClose}>
              关掉
            </button>
          </div>
          <p className="hint">
            如果门牌是别人发给你的，这里会多一句"陈小雨 邀请你加入"，还有一张请柬。除此之外，什么都不会多。
          </p>
        </div>
      </div>
    </div>
  )
}

/** 演示用：把时间变化写进 title，方便截图讲清楚状态。 */
export function usePrototypeTitle(mode: State['mode'], time: TimeOfDay, sceneName: string) {
  useEffect(() => {
    document.title = `${mode === 'memorial' ? '之后' : '现在'} · ${TIME_LABEL[time]} · ${sceneName} | 忆联原型`
  }, [mode, time, sceneName])
}
