// 忆联 · 身后空间与居所 —— 原型数据层
// 结论来源：docs/忆联_身后空间与居所设计_V1.0.md

export type TimeOfDay = 'dawn' | 'dusk' | 'night'
export type Mode = 'living' | 'memorial'

export type Skyline = 'lake' | 'sea' | 'forest' | 'mountain' | 'village' | 'city'
export type HouseStyle = 'study' | 'white' | 'cabin' | 'stone' | 'courtyard' | 'loft'

/** 成套场景：位置与房型绑定，不做自由组合（设计决策 4.3）。 */
export type Scene = {
  id: string
  name: string
  place: string
  kind: string
  note: string
  skyline: Skyline
  house: HouseStyle
  water: boolean
  sky: [string, string]
  far: string
  mid: string
  ground: string
  accent: string
}

export const SCENES: Scene[] = [
  {
    id: 'lake-study',
    name: '湖畔书舍',
    place: '湖畔',
    kind: '书舍',
    note: '清晨的水面很静，适合慢慢讲从前的事',
    skyline: 'lake',
    house: 'study',
    water: true,
    sky: ['#cfe6f2', '#f6e7cb'],
    far: '#8fb2b8',
    mid: '#5f8a86',
    ground: '#6f8f5f',
    accent: '#c8873f',
  },
  {
    id: 'sea-white',
    name: '海边白屋',
    place: '海边',
    kind: '白屋',
    note: '窗子朝海，风一吹，一屋子都是亮的',
    skyline: 'sea',
    house: 'white',
    water: true,
    sky: ['#bfe0f5', '#ffeccd'],
    far: '#9dc6d8',
    mid: '#6fa8bd',
    ground: '#e6d9bd',
    accent: '#3f7fa8',
  },
  {
    id: 'forest-cabin',
    name: '森林木屋',
    place: '森林',
    kind: '木屋',
    note: '木头是自己选的，火塘永远留着一个位置',
    skyline: 'forest',
    house: 'cabin',
    water: false,
    sky: ['#d7ead9', '#f6f0d8'],
    far: '#7fa87c',
    mid: '#4d7a58',
    ground: '#3f6248',
    accent: '#b5713c',
  },
  {
    id: 'mountain-stone',
    name: '高山石屋',
    place: '高山',
    kind: '石屋',
    note: '山上的日子慢，一年只热闹那几回',
    skyline: 'mountain',
    house: 'stone',
    water: false,
    sky: ['#c9d8ee', '#f2e6d6'],
    far: '#8f9fbe',
    mid: '#68789b',
    ground: '#6b6f7a',
    accent: '#a8763f',
  },
  {
    id: 'village-yard',
    name: '乡村院落',
    place: '乡村',
    kind: '院落',
    note: '院里那棵树，是爷爷那一辈种下的',
    skyline: 'village',
    house: 'courtyard',
    water: false,
    sky: ['#f2e2c4', '#fbf3e2'],
    far: '#c2b08c',
    mid: '#9c8a63',
    ground: '#a9a06a',
    accent: '#a8442f',
  },
  {
    id: 'city-loft',
    name: '闹市阁楼',
    place: '闹市',
    kind: '阁楼',
    note: '楼下是整条街的声音，楼上只有一盏灯',
    skyline: 'city',
    house: 'loft',
    water: false,
    sky: ['#d9d3e8', '#f7dfd0'],
    far: '#a79cb8',
    mid: '#7d7391',
    ground: '#8b8496',
    accent: '#d98b3f',
  },
]

export type Photo = { id: string; year: string; caption: string }
export type Story = { id: string; title: string; by: string; text: string; askedBy?: string }
export type Letter = { id: string; to: string; openAt: string; text: string; declined: boolean }
export type Member = { id: string; name: string; relation: string; room: string; private: boolean }
export type Departed = { id: string; name: string; relation: string }
export type Executor = { id: string; name: string; relation: string; stage: ExecutorStage }
export type Instruction = { id: string; label: string; value: string; visible: boolean }

/** 执行人状态机：系统不自动判定死亡（设计红线 3）。 */
export type ExecutorStage = 'none' | 'requested' | 'cooling' | 'active'

export type State = {
  sceneId: string
  time: TimeOfDay
  mode: Mode
  photos: Photo[]
  stories: Story[]
  letters: Letter[]
  members: Member[]
  departed: Departed[]
  executors: Executor[]
  instructions: Instruction[]
  pendingAsk: { by: string; question: string } | null
}

export const HOUSEHOLD = '陈家'
export const OWNER = '陈明远'

export const initialState: State = {
  sceneId: 'lake-study',
  time: 'dawn',
  mode: 'living',
  photos: [
    { id: 'p1', year: '1953', caption: '老屋门前，父亲抱着我' },
    { id: 'p2', year: '1987', caption: '院子里那棵石榴树' },
    { id: 'p3', year: '2001', caption: '第一次全家福' },
  ],
  stories: [
    {
      id: 's1',
      title: '我十七岁那年，走了三天去上学',
      by: OWNER,
      text: '那时候没有车，天不亮就得起来。母亲给我煮了两个鸡蛋，用手帕包着，揣在怀里，走到第二天才舍得吃一个。',
    },
    {
      id: 's2',
      title: '你奶奶最爱唱的那支歌',
      by: OWNER,
      text: '她洗衣服的时候总唱，我在屋里改作业，听着听着就把笔停下来了。歌词我记不全，调子还记得。',
      askedBy: '陈念',
    },
  ],
  letters: [
    {
      id: 'l1',
      to: '陈念',
      openAt: '2027-09-01（念念 18 岁那天）',
      text: '念念，你出生那天下了很大的雨。爷爷想说，往后你遇到再大的雨，家里都有人给你留灯。',
      declined: false,
    },
  ],
  members: [
    { id: 'm1', name: '王秀兰', relation: '妻子', room: '朝南的房', private: false },
    { id: 'm2', name: '陈小雨', relation: '女儿', room: '二楼东间', private: true },
    { id: 'm3', name: '陈松', relation: '儿子', room: '一楼北间', private: true },
    { id: 'm4', name: '陈念', relation: '孙女', room: '小书房', private: true },
  ],
  departed: [{ id: 'd1', name: '陈广福', relation: '父亲' }],
  executors: [{ id: 'e1', name: '陈小雨', relation: '女儿', stage: 'none' }],
  instructions: [
    { id: 'i1', label: '照片', value: '1953 那张，放在最中间', visible: true },
    { id: 'i2', label: '不要做的事', value: '不要放鞭炮，不要请乐队', visible: true },
    { id: 'i3', label: '想说的话', value: '都由秀兰定就好', visible: false },
  ],
  pendingAsk: { by: '陈念', question: '爷爷，你小时候过年都做什么？' },
}

/** 内容阈值：书房书架填满所需的讲述条数（设计 4.4）。 */
export const SHELF_CAPACITY = 8
/** 相册墙容量（设计 4.4）。 */
export const WALL_CAPACITY = 9

export const NAV = [
  { id: 'dwelling', label: '居所' },
  { id: 'rooms', label: '房间' },
  { id: 'letters', label: '留给某人的话' },
  { id: 'keys', label: '钥匙' },
  { id: 'cowrite', label: '一起写' },
  { id: 'bounds', label: '边界' },
] as const

export type NavId = (typeof NAV)[number]['id']
