#!/usr/bin/env node
/**
 * Capture yearly map screenshots from the GlobalWarming climate observatory page.
 * - Starts at 1975, advances to 2015.
 * - Waits for climate tiles to load after each year change.
 * - Screenshots only the map region (#climate-observatory-map).
 * - Saves sequential frames for easy video encoding + per-year named copies.
 */

const puppeteer = require('puppeteer-core');
const fs = require('fs');
const path = require('path');

const TARGET_URL = 'https://jnbigdatatemp.s3.amazonaws.com/index.html';
const FRAMES_DIR = path.resolve(__dirname, 'frames');
const CHROME_PATH = '/usr/bin/google-chrome';

const START_YEAR = 1975;
const END_YEAR = 2015;

function ensureDir(dir) {
  if (!fs.existsSync(dir)) fs.mkdirSync(dir, { recursive: true });
}

function sleep(ms) {
  return new Promise(r => setTimeout(r, ms));
}

async function waitForMapReady(page) {
  // Wait for the map host and at least the leaflet container to be initialized
  await page.waitForSelector('#climate-observatory-map', { timeout: 30000 });
  await page.waitForSelector('#climate-observatory-map .leaflet-container', { timeout: 30000 }).catch(() => {});
  // Give the initial layer a moment
  await sleep(800);
}

async function findSlider(page) {
  // The controls host is populated by the Scala.js app
  await page.waitForSelector('#climate-observatory-controls input[type="range"]', { timeout: 30000 });
  const slider = await page.$('#climate-observatory-controls input[type="range"]');
  if (!slider) throw new Error('Could not find year slider input');
  return slider;
}

async function getSliderBounds(page) {
  return page.evaluate(() => {
    const input = document.querySelector('#climate-observatory-controls input[type="range"]');
    if (!input) return null;
    return {
      min: parseInt(input.min, 10),
      max: parseInt(input.max, 10),
      value: parseInt(input.value, 10)
    };
  });
}

async function setYear(page, slider, year) {
  await page.evaluate((el, y) => {
    el.value = String(y);
    // The app listens to 'change'. Also fire 'input' for good measure (some ranges use it).
    el.dispatchEvent(new Event('input', { bubbles: true }));
    el.dispatchEvent(new Event('change', { bubbles: true }));
  }, slider, year);
}

async function waitForTilesLoaded(page, year, timeoutMs = 20000) {
  const start = Date.now();

  // Primary: wait for network to be relatively idle (no new requests for a short period)
  try {
    await page.waitForNetworkIdle({ idleTime: 350, timeout: Math.min(timeoutMs, 12000) });
  } catch (_) {
    // continue with secondary checks
  }

  // Secondary: no leaflet "loading" tiles + at least a handful of tile images present in the map
  // Also try to ensure the climate layer tiles for this year have started appearing.
  await page.waitForFunction(
    (targetYear) => {
      const map = document.getElementById('climate-observatory-map');
      if (!map) return false;

      const loadingTiles = map.querySelectorAll('.leaflet-tile-loading').length;
      if (loadingTiles > 0) return false;

      const allTiles = map.querySelectorAll('img.leaflet-tile');
      if (allTiles.length < 4) return false; // need a few tiles

      // Heuristic: look for climate layer tiles that mention the target year in their src
      // (they live under target/temperatures/<year>/... or target/deviations/<year>/...)
      let climateTilesForYear = 0;
      for (const img of allTiles) {
        const src = img.src || '';
        if (src.includes(`/temperatures/${targetYear}/`) || src.includes(`/deviations/${targetYear}/`)) {
          // Also ensure the image has actually decoded (naturalWidth > 0)
          if (img.complete && img.naturalWidth > 0) climateTilesForYear++;
        }
      }

      // Accept either several year-specific tiles or (if base tiles dominate) just many complete tiles
      if (climateTilesForYear >= 3) return true;

      // Fallback: if we see many complete tiles and no loading, accept (OSM + climate may interleave)
      let completeCount = 0;
      for (const img of allTiles) {
        if (img.complete && img.naturalWidth > 0) completeCount++;
      }
      return completeCount >= 12;
    },
    { timeout: timeoutMs - (Date.now() - start) > 2000 ? timeoutMs - (Date.now() - start) : 8000 },
    year
  ).catch(() => {
    // Non-fatal; we will still attempt the screenshot
    console.warn(`  [!] Tile readiness check timed out or was inconclusive for ${year}`);
  });

  // Small extra settle time for compositing / CSS transitions
  await sleep(250);
}

async function getMapClip(page) {
  const mapHandle = await page.$('#climate-observatory-map');
  if (!mapHandle) throw new Error('Map element not found for clipping');
  const box = await mapHandle.boundingBox();
  if (!box) throw new Error('Failed to get bounding box for map');
  // Add a tiny inset if desired for clean edges; keep 0 for full map area
  return {
    x: Math.round(box.x),
    y: Math.round(box.y),
    width: Math.round(box.width),
    height: Math.round(box.height),
  };
}

async function captureFrame(page, year, seqIndex) {
  // Additional 2-second wait before capturing the screenshot (as requested)
  // to allow any final rendering, tile compositing, or animations to settle.
  await sleep(2000);

  const clip = await getMapClip(page);

  // Primary sequential name for ffmpeg (zero-padded)
  const seqName = path.join(FRAMES_DIR, `frame_${String(seqIndex).padStart(4, '0')}.png`);
  // Also keep an easy-to-identify per-year copy
  const yearName = path.join(FRAMES_DIR, `year_${year}.png`);

  await page.screenshot({
    path: seqName,
    clip,
    type: 'png',
    omitBackground: false,
  });

  // Duplicate with year name for convenience / inspection
  fs.copyFileSync(seqName, yearName);

  return { seqName, yearName, clip };
}

async function main() {
  ensureDir(FRAMES_DIR);

  console.log('Launching Chrome...');
  const browser = await puppeteer.launch({
    executablePath: CHROME_PATH,
    headless: 'new',
    defaultViewport: { width: 1600, height: 1100 },
    args: [
      '--no-sandbox',
      '--disable-setuid-sandbox',
      '--disable-dev-shm-usage',
      '--disable-gpu',
      '--hide-scrollbars',
      // Helps with some tile loading / webgl quirks in headless
      '--disable-web-security',
      '--allow-running-insecure-content',
    ],
  });

  const page = await browser.newPage();

  // Be generous with timeouts for S3 + tile generation
  page.setDefaultTimeout(45000);
  page.setDefaultNavigationTimeout(45000);

  console.log(`Navigating to ${TARGET_URL} ...`);
  await page.goto(TARGET_URL, { waitUntil: 'networkidle2' });

  await waitForMapReady(page);

  const slider = await findSlider(page);
  const bounds = await getSliderBounds(page);
  console.log('Slider bounds detected:', bounds);

  if (!bounds || bounds.min > START_YEAR || bounds.max < END_YEAR) {
    console.warn(`Warning: Detected bounds [${bounds?.min}..${bounds?.max}] may not fully cover ${START_YEAR}-${END_YEAR}. Proceeding anyway.`);
  }

  // Ensure we start from a clean known state (set to 2015 or max first, then go to 1975)
  // But user wants 1975 first.
  const total = END_YEAR - START_YEAR + 1;
  console.log(`Capturing ${total} frames: ${START_YEAR} -> ${END_YEAR}`);

  let seq = 0;
  const results = [];

  for (let year = START_YEAR; year <= END_YEAR; year++) {
    const label = `[${String(year)}] (${seq + 1}/${total})`;
    process.stdout.write(`${label} setting year... `);

    await setYear(page, slider, year);

    process.stdout.write('waiting for tiles... ');
    await waitForTilesLoaded(page, year);

    process.stdout.write('screenshot... ');
    const { seqName, yearName, clip } = await captureFrame(page, year, seq);
    results.push({ year, seqName, yearName, clip });

    console.log(`saved ${path.basename(seqName)} (${clip.width}x${clip.height})`);
    seq++;
  }

  await browser.close();

  // Write a small manifest for reference / ffmpeg alternative usage
  const manifestPath = path.join(FRAMES_DIR, 'manifest.json');
  fs.writeFileSync(manifestPath, JSON.stringify({
    sourceUrl: TARGET_URL,
    startYear: START_YEAR,
    endYear: END_YEAR,
    frameCount: results.length,
    frames: results.map(r => ({
      year: r.year,
      seq: path.basename(r.seqName),
      file: path.basename(r.yearName),
      size: r.clip,
    })),
  }, null, 2));

  console.log('\nDone.');
  console.log(`Frames written to: ${FRAMES_DIR}`);
  console.log(`Manifest: ${manifestPath}`);
  console.log('Example first/last:', results[0]?.seqName, results.at(-1)?.seqName);
}

main().catch(err => {
  console.error('Capture failed:', err);
  process.exit(1);
});
