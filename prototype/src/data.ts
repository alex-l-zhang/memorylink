// 忆联 · 身后空间与居所 —— 原型数据层
// 结论来源：docs/忆联_身后空间与居所设计_V1.0.md

export type TimeOfDay = 'dawn' | 'dusk' | 'night'
export type Mode = 'living' | 'memorial'

/**
 * 照片场景（V2）：每个成套场景由一张真实照片 + 若干"可点亮的窗口"锚点组成。
 * 锚点坐标是照片的百分比（x/y 为左上角，w/h 为尺寸），与 public/scenes 下 1600×900 的图一一对应，
 * 因此照片按 16:9 铺满画面时不需要再做换算。照片裁切与锚点校准见 prototype/tools/。
 */
export type Spot = { x: number; y: number; w: number; h: number }

/** 屋外的空气感图层：水面薄雾 / 林间光束 / 尘埃 / 城市灯火。 */
export type Air = 'mist' | 'haze' | 'dust' | 'city'

/** 成套场景：位置与房型绑定，不做自由组合（设计决策 4.3）。 */
export type Scene = {
  id: string
  name: string
  place: string
  kind: string
  note: string
  /** 屋外：真实照片（1600×900）。 */
  photo: string
  /** 屋里：真实室内照片（1600×900）。 */
  interior: string
  /** 窗户锚点：新成员加入就多亮一扇（设计 4.4）。 */
  lights: Spot[]
  /** 门口那盏灯的锚点，切到"之后"时亮起。 */
  lamp: Spot
  /** 长明灯的落点（每位故人一盏）。 */
  candle: { x: number; y: number }
  air: Air
  accent: string
  /** 照片来源，用于原型署名。 */
  credit: string
}

export const SCENES: Scene[] = [
  {
    id: 'lake-study',
    name: '湖畔书舍',
    place: '湖畔',
    kind: '书舍',
    note: '清晨的水面很静，适合慢慢讲从前的事',
    photo: '/scenes/lake-study.jpg',
    interior: '/scenes/in-lake.jpg',
    lights: [
      { x: 28.4, y: 24.6, w: 3.4, h: 4.6 },
      { x: 28.2, y: 32, w: 3.6, h: 4.4 },
      { x: 25, y: 40.6, w: 3.8, h: 4.8 },
      { x: 31.2, y: 40.6, w: 3.8, h: 4.8 },
    ],
    lamp: { x: 32.2, y: 47.4, w: 4.2, h: 4.6 },
    candle: { x: 44, y: 87 },
    air: 'mist',
    accent: '#e2a24d',
    credit: 'Unsplash',
  },
  {
    id: 'sea-white',
    name: '海边白屋',
    place: '海边',
    kind: '白屋',
    note: '窗子朝海，风一吹，一屋子都是亮的',
    photo: '/scenes/sea-white.jpg',
    interior: '/scenes/in-sea.jpg',
    lights: [
      { x: 42, y: 46.5, w: 2.6, h: 3.8 },
      { x: 60.5, y: 47, w: 2.4, h: 3.4 },
      { x: 85, y: 45, w: 2.6, h: 4 },
      { x: 92, y: 46.5, w: 2.6, h: 4 },
    ],
    lamp: { x: 44, y: 50.5, w: 3, h: 3 },
    candle: { x: 44, y: 87 },
    air: 'mist',
    accent: '#7fa9c4',
    credit: 'Unsplash',
  },
  {
    id: 'forest-cabin',
    name: '森林木屋',
    place: '森林',
    kind: '木屋',
    note: '木头是自己选的，火塘永远留着一个位置',
    photo: '/scenes/forest-cabin.jpg',
    interior: '/scenes/in-forest.jpg',
    lights: [
      { x: 15.2, y: 58.5, w: 4.8, h: 10.5 },
      { x: 21.4, y: 59, w: 4.2, h: 9.5 },
      { x: 32.4, y: 57.5, w: 4.8, h: 11.5 },
      { x: 48.2, y: 58.5, w: 5.2, h: 9 },
    ],
    lamp: { x: 56.5, y: 68.5, w: 4.4, h: 4.4 },
    candle: { x: 44, y: 87 },
    air: 'haze',
    accent: '#8fae7a',
    credit: 'Unsplash',
  },
  {
    id: 'mountain-stone',
    name: '高山石屋',
    place: '高山',
    kind: '石屋',
    note: '山上的日子慢，一年只热闹那几回',
    photo: '/scenes/mountain-stone.jpg',
    interior: '/scenes/in-mountain.jpg',
    lights: [
      { x: 75.4, y: 25, w: 4, h: 5.6 },
      { x: 74.6, y: 44.5, w: 4.6, h: 11.5 },
      { x: 89.4, y: 28, w: 3.6, h: 6.5 },
      { x: 71.6, y: 79.5, w: 3.2, h: 4.4 },
    ],
    lamp: { x: 76.6, y: 39.5, w: 4.4, h: 4.4 },
    candle: { x: 44, y: 87 },
    air: 'dust',
    accent: '#c8955a',
    credit: 'Unsplash',
  },
  {
    id: 'village-yard',
    name: '乡村院落',
    place: '乡村',
    kind: '院落',
    note: '院里那棵树，是爷爷那一辈种下的',
    photo: '/scenes/village-yard.jpg',
    interior: '/scenes/in-village.jpg',
    lights: [
      { x: 57.5, y: 21.5, w: 22, h: 6 },
      { x: 56.5, y: 30, w: 11.5, h: 18 },
      { x: 69.5, y: 30, w: 11.5, h: 18 },
      { x: 10.5, y: 28, w: 6.5, h: 16 },
    ],
    lamp: { x: 12, y: 50, w: 4, h: 4 },
    candle: { x: 44, y: 87 },
    air: 'dust',
    accent: '#b8653f',
    credit: 'Unsplash',
  },
  {
    id: 'city-loft',
    name: '闹市阁楼',
    place: '闹市',
    kind: '阁楼',
    note: '楼下是整条街的声音，楼上只有一盏灯',
    photo: '/scenes/city-loft.jpg',
    interior: '/scenes/in-city.jpg',
    lights: [
      { x: 25.5, y: 77.5, w: 3.6, h: 2.8 },
      { x: 37.5, y: 83.5, w: 3.6, h: 2.8 },
      { x: 54.5, y: 79.5, w: 3.6, h: 2.8 },
      { x: 67.5, y: 73.5, w: 3.6, h: 2.8 },
    ],
    lamp: { x: 62, y: 87.5, w: 4.2, h: 3.4 },
    candle: { x: 44, y: 87 },
    air: 'city',
    accent: '#d98b3f',
    credit: 'Unsplash',
  },
]

/** 家人的照片：原型用真实图片，替换成用户上传的照片即可。 */
export const ALBUM = [
  '/album/01.jpg',
  '/album/02.jpg',
  '/album/03.jpg',
  '/album/04.jpg',
  '/album/05.jpg',
  '/album/06.jpg',
  '/album/07.jpg',
  '/album/08.jpg',
]

export type Photo = { id: string; year: string; caption: string; img: string }
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
    { id: 'p1', year: '1953', caption: '老屋门前，父亲抱着我', img: '/album/01.jpg' },
    { id: 'p2', year: '1987', caption: '院子里那棵石榴树', img: '/album/03.jpg' },
    { id: 'p3', year: '2001', caption: '第一次全家福', img: '/album/02.jpg' },
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
