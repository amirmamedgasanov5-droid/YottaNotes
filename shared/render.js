// usage: node render.js <episode-dir> <total-seconds> <out-dir> [fps=24] [workers=4]
const { chromium } = require('playwright');
const [dir, total, out, fps = 24, workers = 4] = process.argv.slice(2);
const frames = Math.round(+fps * +total);
(async () => {
  require('fs').mkdirSync(out, { recursive: true });
  const b = await chromium.launch({ args: ['--disable-threaded-animation'] });
  const per = Math.ceil(frames / +workers);
  await Promise.all([...Array(+workers).keys()].map(async w => {
    const ctx = await b.newContext({ viewport: { width: +(process.env.VW || 540), height: +(process.env.VH || 960) }, deviceScaleFactor: +(process.env.DSF || 2) });
    const p = await ctx.newPage();
    await p.goto(`file:///home/user/YottaNotes/${dir}/index.html`);
    await p.evaluate(() => window.seek(0)); await p.waitForTimeout(400);
    for (let f = w * per; f < Math.min(frames, (w + 1) * per); f++) {
      await p.evaluate(t => window.seek(t), f / +fps);
      await p.screenshot({ path: `${out}/${String(f).padStart(5, '0')}.jpg`, type: 'jpeg', quality: 90 });
    }
    await ctx.close();
  }));
  await b.close();
  console.log('done', frames);
})();
