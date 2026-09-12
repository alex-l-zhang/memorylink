/**
 * 原型的视觉与机制冒烟测试（需要无头浏览器）。
 *
 * 为什么需要它：jsdom 只能验文字和点击，验证不了"照片有没有加载出来""窗灯有没有亮在窗户上"。
 * 这个脚本用真实 Chromium 打开原型，检查底图、亮灯数、相框与书架随内容增长、"之后"的灯，
 * 并断言没有加载失败的图片、没有控制台错误。
 *
 * 运行（playwright 不是原型依赖，装在别处即可）：
 *   npm i playwright && npx playwright install chromium
 *   node prototype/tools/visual_smoke.cjs http://127.0.0.1:5190/
 */
const { chromium } = require(process.env.PLAYWRIGHT_PATH || 'playwright')
const assert = (c, m) => console.log(`${c ? 'PASS' : 'FAIL'}  ${m}`) || (!c && (process.exitCode = 1))
;(async () => {
  const url = process.argv[2] || 'http://127.0.0.1:5190/'
  const browser = await chromium.launch()
  const page = await browser.newPage({ viewport: { width: 1440, height: 900 } })
  const errors = []
  page.on('console', (m) => m.type() === 'error' && errors.push(m.text()))
  page.on('pageerror', (e) => errors.push(String(e)))
  await page.goto(url, { waitUntil: 'networkidle' })
  await page.waitForTimeout(700)
  const click = async (n) => { await page.getByRole('button', { name: n, exact: true }).first().click(); await page.waitForTimeout(350) }

  assert(await page.locator('.scene-base').first().getAttribute('src') === '/scenes/lake-study.jpg', '屋外背景是真实照片')
  const lit0 = await page.locator('.scene-light.on').count()
  assert(lit0 === 4, `初始亮灯窗 ${lit0} 个（家人 5 → 上限 4）`)

  await click('屋里')
  const frames0 = await page.locator('.frame:not(.is-empty)').count()
  const books0 = await page.locator('.book.on').count()
  assert(frames0 === 3, `屋里：墙上 ${frames0} 张照片`)
  assert(books0 === 6, `屋里：书架 ${books0} 本书`)

  await page.getByRole('button', { name: '居所', exact: true }).first().click()
  await page.waitForTimeout(300)
  await click('墙上多一张照片')
  await click('书架多一段讲述')
  await click('家里多一个人')
  await click('屋里')
  const frames1 = await page.locator('.frame:not(.is-empty)').count()
  const books1 = await page.locator('.book.on').count()
  assert(frames1 === 4, `放照片后：墙上 ${frames1} 张（内容驱动）`)
  assert(books1 === 9, `讲述后：书架 ${books1} 本（内容驱动）`)

  await page.getByRole('button', { name: '居所', exact: true }).first().click()
  await click('屋外')
  const lit1 = await page.locator('.scene-light.on').count()
  assert(lit1 === lit0, `家人加入后亮灯仍受锚点上限约束（${lit1}）`)

  await click('之后')
  assert(await page.locator('.scene-lamp').count() === 1, '"之后"：门口亮起一盏灯')
  assert(await page.locator('.scene-candle').count() >= 1, '"之后"：故人处有长明灯')
  await click('夜晚')
  assert((await page.locator('.scene').first().getAttribute('class')).includes('tone-night'), '时段切换生效')

  await click('换个地方')
  await page.locator('.pick', { hasText: '乡村院落' }).first().click()
  await page.waitForTimeout(600)
  assert((await page.locator('.scene-base').first().getAttribute('src')) === '/scenes/village-yard.jpg', '换地方换的是真实照片')

  const broken = await page.evaluate(() => [...document.images].filter((i) => i.complete && i.naturalWidth === 0).map((i) => i.src))
  assert(broken.length === 0, `没有加载失败的图片${broken.length ? '：' + broken.join(',') : ''}`)
  assert(errors.length === 0, `无控制台错误${errors.length ? '：' + errors.slice(0, 2).join(' | ') : ''}`)
  await browser.close()
})()
