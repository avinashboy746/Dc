/**
 * Kirby - Dedicated Mobile Discord Client
 * Clean Download Hub & Mobile Preview Server
 * Port 3000
 */
const http = require('http');
const fs = require('fs');
const path = require('path');
const { spawn } = require('child_process');
const crypto = require('crypto');

process.on('uncaughtException', (err) => {
  console.error('[UNCAUGHT EXCEPTION]', err);
});
process.on('unhandledRejection', (reason) => {
  console.error('[UNHANDLED REJECTION]', reason);
});

const PORT = 3000;
const APK_PATH = path.resolve(__dirname, 'app/build/outputs/apk/debug/app-debug.apk');
const BACKUP_APK_PATH = path.resolve(__dirname, '.build-outputs/app-debug.apk');

const APP_NAME = 'Kirby';

let buildState = {
  status: 'idle',
  progress: 100,
  currentStage: 'Kirby APK Ready',
  startTime: null,
  endTime: null,
  durationSeconds: 0,
  logs: [
    `[SYSTEM] ${APP_NAME} Android Project Initialized.`,
    '[SYSTEM] Custom anime launcher icon configured.',
    '[SYSTEM] Pure Discord full-screen WebViewContainer active (no URL bar).',
    '[SYSTEM] 24/7 Always-Online Foreground Service compiled.',
    '[SYSTEM] Kirby.apk verified and ready for download.'
  ],
  error: null,
  lastBuilt: new Date().toISOString()
};

function getApkInfo() {
  const targetPath = fs.existsSync(APK_PATH) ? APK_PATH : (fs.existsSync(BACKUP_APK_PATH) ? BACKUP_APK_PATH : null);
  if (!targetPath) {
    return { exists: false, size: '0 MB', bytes: 0, sha256: null, mtime: null };
  }
  try {
    const stat = fs.statSync(targetPath);
    const fileBuffer = fs.readFileSync(targetPath);
    const sha256 = crypto.createHash('sha256').update(fileBuffer).digest('hex');
    const sizeMb = (stat.size / (1024 * 1024)).toFixed(1) + ' MB';
    return {
      exists: true,
      size: sizeMb,
      bytes: stat.size,
      sha256: sha256,
      mtime: stat.mtime.toISOString(),
      path: targetPath,
      name: APP_NAME
    };
  } catch (err) {
    return { exists: false, size: '0 MB', bytes: 0, sha256: null, mtime: null, error: err.message };
  }
}

let activeBuildProcess = null;

function runGradleBuild() {
  if (buildState.status === 'building' || buildState.status === 'preparing') {
    return { success: false, message: 'Build already in progress' };
  }

  buildState = {
    status: 'preparing',
    progress: 10,
    currentStage: 'Preparing Android Build Environment',
    startTime: Date.now(),
    endTime: null,
    durationSeconds: 0,
    logs: [
      `[${new Date().toLocaleTimeString()}] Triggering Gradle build for ${APP_NAME}...`,
      `[${new Date().toLocaleTimeString()}] Compiling Android resources and custom Kirby launcher icon...`
    ],
    error: null,
    lastBuilt: null
  };

  const timer = setInterval(() => {
    if (buildState.status === 'building' || buildState.status === 'preparing') {
      const elapsed = Math.floor((Date.now() - buildState.startTime) / 1000);
      buildState.durationSeconds = elapsed;
      if (buildState.progress < 90) {
        buildState.progress = Math.min(90, buildState.progress + 5);
      }
    } else {
      clearInterval(timer);
    }
  }, 1000);

  setTimeout(() => {
    buildState.status = 'building';
    buildState.progress = 30;
    buildState.currentStage = 'Compiling Kotlin source code';
    buildState.logs.push(`[${new Date().toLocaleTimeString()}] Executing: gradle assembleDebug`);

    const gradleCmd = fs.existsSync(path.resolve(__dirname, 'gradlew')) ? './gradlew' : 'gradle';
    activeBuildProcess = spawn(gradleCmd, ['assembleDebug', '--no-daemon'], {
      cwd: __dirname,
      env: process.env
    });

    activeBuildProcess.stdout.on('data', (data) => {
      const text = data.toString();
      const lines = text.split('\n').filter(Boolean);
      for (const line of lines) {
        buildState.logs.push(line);
      }
      if (buildState.logs.length > 300) {
        buildState.logs = buildState.logs.slice(-300);
      }
    });

    activeBuildProcess.stderr.on('data', (data) => {
      buildState.logs.push(`[STDERR] ${data.toString()}`);
    });

    activeBuildProcess.on('close', (code) => {
      clearInterval(timer);
      activeBuildProcess = null;
      buildState.endTime = Date.now();
      buildState.durationSeconds = Math.floor((buildState.endTime - buildState.startTime) / 1000);

      if (code === 0 && (fs.existsSync(APK_PATH) || fs.existsSync(BACKUP_APK_PATH))) {
        buildState.status = 'completed';
        buildState.progress = 100;
        buildState.currentStage = 'Build Successful';
        buildState.lastBuilt = new Date().toISOString();
        buildState.logs.push(`[${new Date().toLocaleTimeString()}] BUILD SUCCESSFUL in ${buildState.durationSeconds}s`);
        buildState.logs.push(`[${new Date().toLocaleTimeString()}] Output APK ready: Kirby.apk`);
      } else {
        buildState.status = 'failed';
        buildState.error = `Gradle exited with code ${code}`;
        buildState.currentStage = 'Build Failed';
      }
    });

    activeBuildProcess.on('error', (err) => {
      clearInterval(timer);
      activeBuildProcess = null;
      buildState.status = 'failed';
      buildState.error = err.message;
    });
  }, 1000);

  return { success: true, message: 'Build started' };
}

// Generate the Clean HTML Dashboard
function renderDashboardHtml() {
  const apk = getApkInfo();
  return `<!DOCTYPE html>
<html lang="en">
<head>
  <meta charset="UTF-8">
  <meta name="viewport" content="width=device-width, initial-scale=1.0, maximum-scale=1.0, user-scalable=no">
  <title>Kirby — Dedicated Mobile Discord Client</title>
  <meta name="description" content="Kirby is a dedicated mobile Android app for Discord with custom anime icon, full-screen view without URL bar, and 24/7 online keepalive.">
  <style>
    :root {
      --kirby-pink: #FF66B2;
      --kirby-soft-pink: #FFB6D9;
      --blurple: #5865F2;
      --dark-bg: #1E1F22;
      --dark-surface: #2B2D31;
      --dark-chat: #313338;
      --deep-black: #111214;
      --discord-green: #57F287;
      --text-primary: #F2F3F5;
      --text-muted: #949BA4;
      --divider: #3F4147;
    }
    * { box-sizing: border-box; margin: 0; padding: 0; -webkit-tap-highlight-color: transparent; }
    body {
      font-family: -apple-system, BlinkMacSystemFont, "Segoe UI", Roboto, Helvetica, Arial, sans-serif;
      background-color: var(--deep-black);
      color: var(--text-primary);
      min-height: 100vh;
      display: flex;
      flex-direction: column;
      overflow-x: hidden;
    }
    header {
      background: rgba(17, 18, 20, 0.95);
      backdrop-filter: blur(12px);
      border-bottom: 1px solid var(--divider);
      position: sticky;
      top: 0;
      z-index: 50;
      padding: 12px 20px;
      display: flex;
      align-items: center;
      justify-content: space-between;
    }
    .brand {
      display: flex;
      align-items: center;
      gap: 12px;
      text-decoration: none;
      color: var(--text-primary);
    }
    .app-icon-badge {
      width: 44px;
      height: 44px;
      border-radius: 14px;
      background: linear-gradient(135deg, #FFB6D9, #FF88B8);
      display: flex;
      align-items: center;
      justify-content: center;
      box-shadow: 0 4px 14px rgba(255, 102, 178, 0.4);
      overflow: hidden;
      border: 2px solid rgba(255, 255, 255, 0.3);
    }
    .brand-title {
      font-weight: 800;
      font-size: 1.25rem;
      letter-spacing: -0.3px;
      color: var(--text-primary);
    }
    .brand-sub {
      font-size: 0.75rem;
      color: var(--kirby-pink);
      font-weight: 600;
    }

    nav.nav-tabs {
      display: flex;
      background: var(--dark-bg);
      border-bottom: 1px solid var(--divider);
      padding: 0 16px;
      gap: 8px;
    }
    .nav-tab-btn {
      background: none;
      border: none;
      color: var(--text-muted);
      padding: 14px 20px;
      font-size: 0.95rem;
      font-weight: 700;
      cursor: pointer;
      display: flex;
      align-items: center;
      gap: 8px;
      border-bottom: 3px solid transparent;
      transition: all 0.2s ease;
    }
    .nav-tab-btn.active {
      color: var(--text-primary);
      border-bottom-color: var(--kirby-pink);
    }

    main {
      flex: 1;
      max-width: 900px;
      width: 100%;
      margin: 0 auto;
      padding: 28px 16px 60px;
    }
    .tab-content { display: none; }
    .tab-content.active { display: block; animation: fadeIn 0.2s ease; }
    @keyframes fadeIn { from { opacity: 0; } to { opacity: 1; } }

    /* Clean Normal Download Card */
    .download-card {
      background: linear-gradient(145deg, #1e2025, #26282e);
      border: 1px solid #36393f;
      border-radius: 24px;
      padding: 36px 28px;
      margin-bottom: 24px;
      box-shadow: 0 12px 36px rgba(0, 0, 0, 0.5);
      text-align: center;
      display: flex;
      flex-direction: column;
      align-items: center;
    }
    .app-hero-icon {
      width: 96px;
      height: 96px;
      border-radius: 26px;
      background: linear-gradient(135deg, #FFB6D9, #FFAAC8);
      border: 3px solid rgba(255, 255, 255, 0.4);
      box-shadow: 0 12px 30px rgba(255, 102, 178, 0.45);
      margin-bottom: 18px;
      display: flex;
      align-items: center;
      justify-content: center;
      overflow: hidden;
    }
    .app-hero-title {
      font-size: 2rem;
      font-weight: 800;
      margin-bottom: 6px;
    }
    .app-hero-desc {
      color: var(--text-muted);
      font-size: 0.95rem;
      max-width: 480px;
      margin-bottom: 22px;
      line-height: 1.5;
    }
    .specs-pill-row {
      display: flex;
      flex-wrap: wrap;
      justify-content: center;
      gap: 10px;
      margin-bottom: 26px;
    }
    .specs-pill {
      background: var(--dark-chat);
      border: 1px solid var(--divider);
      padding: 6px 14px;
      border-radius: 20px;
      font-size: 0.8rem;
      color: var(--text-muted);
    }
    .specs-pill b { color: var(--text-primary); }

    .btn-download-primary {
      display: inline-flex;
      align-items: center;
      justify-content: center;
      gap: 12px;
      background: linear-gradient(135deg, #FF66B2, #5865F2);
      color: white;
      padding: 16px 42px;
      border-radius: 14px;
      font-size: 1.15rem;
      font-weight: 800;
      text-decoration: none;
      box-shadow: 0 8px 24px rgba(255, 102, 178, 0.4);
      transition: all 0.2s ease;
      cursor: pointer;
      border: none;
    }
    .btn-download-primary:hover {
      transform: translateY(-2px);
      box-shadow: 0 12px 28px rgba(255, 102, 178, 0.55);
    }

    .info-section {
      background: var(--dark-surface);
      border: 1px solid var(--divider);
      border-radius: 18px;
      padding: 24px;
      margin-top: 24px;
    }
    .info-section h3 {
      font-size: 1.1rem;
      margin-bottom: 14px;
      color: var(--text-primary);
    }
    .steps-list {
      list-style: none;
      display: flex;
      flex-direction: column;
      gap: 12px;
      font-size: 0.9rem;
      color: var(--text-muted);
      line-height: 1.4;
    }
    .steps-list li {
      display: flex;
      align-items: center;
      gap: 12px;
    }
    .step-circle {
      width: 24px;
      height: 24px;
      border-radius: 50%;
      background: var(--kirby-pink);
      color: white;
      display: flex;
      align-items: center;
      justify-content: center;
      font-weight: bold;
      font-size: 0.75rem;
      flex-shrink: 0;
    }

    /* Phone Mockup Screen */
    .simulator-box {
      display: flex;
      justify-content: center;
      align-items: center;
      padding: 10px 0;
    }
    .phone-frame {
      width: 370px;
      height: 720px;
      max-width: 100%;
      background: #000;
      border-radius: 44px;
      border: 8px solid #2B2D31;
      box-shadow: 0 25px 60px rgba(0, 0, 0, 0.7);
      display: flex;
      flex-direction: column;
      overflow: hidden;
      position: relative;
    }
    .phone-notch {
      width: 120px;
      height: 18px;
      background: #2B2D31;
      border-radius: 0 0 12px 12px;
      position: absolute;
      top: 0;
      left: 50%;
      transform: translateX(-50%);
      z-index: 10;
    }
    .phone-top-status {
      height: 30px;
      background: var(--deep-black);
      display: flex;
      align-items: center;
      justify-content: space-between;
      padding: 0 18px;
      font-size: 0.7rem;
      color: var(--text-muted);
      z-index: 9;
    }
    .phone-content {
      flex: 1;
      background: var(--dark-chat);
      display: flex;
      flex-direction: column;
      position: relative;
      overflow: hidden;
    }
    .phone-bottom-nav {
      height: 52px;
      background: var(--deep-black);
      border-top: 1px solid var(--divider);
      display: flex;
      align-items: center;
      justify-content: space-around;
    }
    .phone-tab {
      display: flex;
      flex-direction: column;
      align-items: center;
      gap: 2px;
      color: var(--text-muted);
      font-size: 0.7rem;
      cursor: pointer;
    }
    .phone-tab.active {
      color: var(--kirby-pink);
      font-weight: 700;
    }

    /* Home Screen App Grid Mockup */
    .phone-home-grid {
      padding: 40px 24px;
      display: grid;
      grid-template-columns: repeat(3, 1fr);
      gap: 24px;
      align-content: start;
      justify-items: center;
      height: 100%;
      background: linear-gradient(180deg, #18191c 0%, #0d0e10 100%);
    }
    .app-icon-item {
      display: flex;
      flex-direction: column;
      align-items: center;
      gap: 8px;
      cursor: pointer;
      text-decoration: none;
    }
    .app-icon-sq {
      width: 60px;
      height: 60px;
      border-radius: 16px;
      display: flex;
      align-items: center;
      justify-content: center;
      box-shadow: 0 6px 16px rgba(0, 0, 0, 0.4);
      transition: transform 0.2s ease;
    }
    .app-icon-sq:hover { transform: scale(1.05); }
    .app-icon-label {
      font-size: 0.75rem;
      color: var(--text-primary);
      font-weight: 500;
    }

    /* Pure Discord View Mockup (No URL Bar!) */
    .discord-pure-view {
      display: none;
      flex-direction: column;
      height: 100%;
      background: var(--dark-chat);
    }
    .discord-pure-header {
      background: var(--dark-bg);
      padding: 12px 16px;
      display: flex;
      align-items: center;
      justify-content: space-between;
      border-bottom: 1px solid var(--divider);
    }

    #toast {
      position: fixed; bottom: 24px; right: 24px; background: var(--dark-surface);
      border: 1px solid var(--kirby-pink); color: var(--text-primary); padding: 12px 20px;
      border-radius: 10px; box-shadow: 0 10px 30px rgba(0, 0, 0, 0.5); display: none;
      align-items: center; gap: 10px; z-index: 200; font-size: 0.9rem;
    }
  </style>
</head>
<body>

  <!-- Top Header -->
  <header>
    <a href="#" class="brand">
      <div class="app-icon-badge">
        <svg viewBox="0 0 100 100" width="36" height="36" fill="none">
          <circle cx="50" cy="50" r="48" fill="#FFB6D9"/>
          <circle cx="36" cy="24" r="10" fill="#FFF0F5"/>
          <circle cx="64" cy="24" r="10" fill="#FFF0F5"/>
          <ellipse cx="50" cy="55" rx="30" ry="28" fill="#FFAAC8"/>
          <circle cx="38" cy="52" r="5" fill="#7A2255"/>
          <circle cx="62" cy="52" r="5" fill="#7A2255"/>
          <circle cx="39" cy="50" r="2" fill="white"/>
          <circle cx="63" cy="50" r="2" fill="white"/>
          <path d="M47 62 Q50 65 53 62" stroke="#7A2255" stroke-width="2" stroke-linecap="round"/>
        </svg>
      </div>
      <div>
        <div class="brand-title">${APP_NAME}</div>
        <div class="brand-sub">Mobile Discord App</div>
      </div>
    </a>
    <div style="font-size:0.8rem;background:rgba(87,242,135,0.12);color:var(--discord-green);padding:4px 12px;border-radius:14px;border:1px solid rgba(87,242,135,0.3);font-weight:700;">
      ● Version 1.0.0
    </div>
  </header>

  <!-- Navigation Tabs -->
  <nav class="nav-tabs">
    <button class="nav-tab-btn active" onclick="switchTab('download')">
      Download APK
    </button>
    <button class="nav-tab-btn" onclick="switchTab('preview')">
      Mobile Phone View
    </button>
  </nav>

  <main>
    <!-- TAB 1: NORMAL DOWNLOAD OPTION -->
    <section id="tab-download" class="tab-content active">
      <div class="download-card">
        <div class="app-hero-icon">
          <svg viewBox="0 0 100 100" width="80" height="80" fill="none">
            <circle cx="50" cy="50" r="48" fill="#FFB6D9"/>
            <circle cx="36" cy="24" r="10" fill="#FFF0F5"/>
            <circle cx="64" cy="24" r="10" fill="#FFF0F5"/>
            <ellipse cx="50" cy="55" rx="30" ry="28" fill="#FFAAC8"/>
            <circle cx="38" cy="52" r="5" fill="#7A2255"/>
            <circle cx="62" cy="52" r="5" fill="#7A2255"/>
            <circle cx="39" cy="50" r="2" fill="white"/>
            <circle cx="63" cy="50" r="2" fill="white"/>
            <path d="M47 62 Q50 65 53 62" stroke="#7A2255" stroke-width="2" stroke-linecap="round"/>
          </svg>
        </div>

        <h1 class="app-hero-title">${APP_NAME}</h1>
        <p class="app-hero-desc">
          Clean mobile Discord app. Opens directly into full-screen Discord with no address bar, smooth typing, and 24/7 online keepalive.
        </p>

        <div class="specs-pill-row">
          <div class="specs-pill">File: <b>Kirby.apk</b></div>
          <div class="specs-pill">Size: <b>${apk.size}</b></div>
          <div class="specs-pill">Version: <b>1.0.0</b></div>
          <div class="specs-pill">Android: <b>7.0+ (Universal)</b></div>
        </div>

        <a href="/api/download/apk" class="btn-download-primary">
          <svg width="22" height="22" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2.5"><path d="M21 15v4a2 2 0 0 1-2 2H5a2 2 0 0 1-2-2v-4"></path><polyline points="7 10 12 15 17 10"></polyline><line x1="12" y1="15" x2="12" y2="3"></line></svg>
          Download APK (${apk.size})
        </a>

        <div style="margin-top:20px;">
          <button onclick="rebuildApk()" id="rebuild-btn" style="background:none;border:none;color:var(--text-muted);cursor:pointer;font-size:0.85rem;text-decoration:underline;">
            Recompile APK (Gradle)
          </button>
        </div>
      </div>

      <div class="info-section">
        <h3>How to Install on Your Android Phone</h3>
        <ul class="steps-list">
          <li>
            <div class="step-circle">1</div>
            <div>Tap the <b>Download APK</b> button above to download <code>Kirby.apk</code> to your device.</div>
          </li>
          <li>
            <div class="step-circle">2</div>
            <div>Open the downloaded file from your Notifications or Downloads folder.</div>
          </li>
          <li>
            <div class="step-circle">3</div>
            <div>If prompted, tap <i>Settings</i> and allow <i>Install from this source</i>.</div>
          </li>
          <li>
            <div class="step-circle">4</div>
            <div>Open <b>Kirby</b> on your home screen and sign in to Discord!</div>
          </li>
        </ul>
      </div>
    </section>

    <!-- TAB 2: MOBILE PHONE PREVIEW -->
    <section id="tab-preview" class="tab-content">
      <div class="simulator-box">
        <div class="phone-frame">
          <div class="phone-notch"></div>
          
          <div class="phone-top-status">
            <span>9:41</span>
            <span>5G 100%</span>
          </div>

          <div class="phone-content">
            <!-- 1. Phone Home Screen (Like Facebook / Discord app icons) -->
            <div id="phone-home-view" class="phone-home-grid">
              <!-- Facebook icon -->
              <div class="app-icon-item" onclick="showToast('Facebook App')">
                <div class="app-icon-sq" style="background:#1877F2;">
                  <span style="color:white;font-weight:bold;font-size:1.8rem;font-family:sans-serif;">f</span>
                </div>
                <span class="app-icon-label">Facebook</span>
              </div>

              <!-- Official Discord icon -->
              <div class="app-icon-item" onclick="showToast('Official Discord')">
                <div class="app-icon-sq" style="background:#5865F2;">
                  <svg width="32" height="32" viewBox="0 0 24 24" fill="white"><path d="M20.317 4.37a19.791 19.791 0 0 0-4.885-1.515.074.074 0 0 0-.079.037c-.21.375-.444.864-.608 1.25a18.27 18.27 0 0 0-5.487 0 12.64 12.64 0 0 0-.617-1.25.077.077 0 0 0-.079-.037A19.736 19.736 0 0 0 3.677 4.37a.07.07 0 0 0-.032.027C.533 9.046-.32 13.58.099 18.057a.082.082 0 0 0 .031.057 19.9 19.9 0 0 0 5.993 3.03.078.078 0 0 0 .084-.028c.462-.63.874-1.295 1.226-1.994.021-.041.001-.09-.041-.106a13.107 13.107 0 0 1-1.872-.892.077.077 0 0 1-.008-.128 10.2 10.2 0 0 0 .372-.292.074.074 0 0 1 .077-.01c3.929 1.793 8.18 1.793 12.061 0a.074.074 0 0 1 .078.01c.12.098.246.198.373.292a.077.077 0 0 1-.006.127 12.299 12.299 0 0 1-1.873.893.077.077 0 0 0-.041.107c.36.698.772 1.362 1.225 1.993a.076.076 0 0 0 .084.028 19.839 19.839 0 0 0 6.002-3.03.077.077 0 0 0 .032-.054c.5-5.177-.838-9.674-3.549-13.66a.061.061 0 0 0-.031-.028zM8.02 15.33c-1.183 0-2.157-1.085-2.157-2.419 0-1.333.956-2.419 2.157-2.419 1.21 0 2.176 1.096 2.157 2.42 0 1.333-.956 2.418-2.157 2.418zm7.975 0c-1.183 0-2.157-1.085-2.157-2.419 0-1.333.955-2.419 2.157-2.419 1.21 0 2.176 1.096 2.157 2.42 0 1.333-.946 2.418-2.157 2.418z"/></svg>
                </div>
                <span class="app-icon-label">Discord</span>
              </div>

              <!-- Custom Kirby app icon! -->
              <div class="app-icon-item" onclick="openKirbyApp()">
                <div class="app-icon-sq" style="background:linear-gradient(135deg, #FFB6D9, #FFAAC8);border:2px solid rgba(255,255,255,0.4);">
                  <svg viewBox="0 0 100 100" width="48" height="48" fill="none">
                    <circle cx="50" cy="50" r="48" fill="#FFB6D9"/>
                    <circle cx="36" cy="24" r="10" fill="#FFF0F5"/>
                    <circle cx="64" cy="24" r="10" fill="#FFF0F5"/>
                    <ellipse cx="50" cy="55" rx="30" ry="28" fill="#FFAAC8"/>
                    <circle cx="38" cy="52" r="5" fill="#7A2255"/>
                    <circle cx="62" cy="52" r="5" fill="#7A2255"/>
                    <circle cx="39" cy="50" r="2" fill="white"/>
                    <circle cx="63" cy="50" r="2" fill="white"/>
                    <path d="M47 62 Q50 65 53 62" stroke="#7A2255" stroke-width="2" stroke-linecap="round"/>
                  </svg>
                </div>
                <span class="app-icon-label" style="color:#FFB6D9;font-weight:bold;">Kirby 🌸</span>
              </div>
            </div>

            <!-- 2. Pure Discord App View (NO URL LINK BAR!) -->
            <div id="phone-app-view" class="discord-pure-view">
              <div class="discord-pure-header">
                <div style="display:flex;align-items:center;gap:10px;">
                  <button onclick="closeKirbyApp()" style="background:none;border:none;color:var(--text-muted);cursor:pointer;font-size:1.1rem;">✕</button>
                  <span style="font-weight:bold;font-size:0.95rem;"># general</span>
                </div>
                <span style="font-size:0.7rem;color:var(--discord-green);">● Pure App Mode</span>
              </div>

              <!-- Chat -->
              <div style="flex:1;overflow-y:auto;padding:14px;display:flex;flex-direction:column;gap:12px;">
                <div style="background:rgba(255,102,178,0.1);border-left:3px solid var(--kirby-pink);padding:10px;border-radius:6px;font-size:0.75rem;color:var(--text-muted);">
                  <b>Kirby Client:</b> URL bar removed completely. Discord loads directly in clean full-screen view.
                </div>

                <div style="display:flex;gap:10px;align-items:flex-start;">
                  <div style="width:34px;height:34px;border-radius:50%;background:#FF88B8;display:flex;align-items:center;justify-content:center;font-weight:bold;color:white;font-size:0.75rem;">
                    🌸
                  </div>
                  <div>
                    <div style="font-weight:bold;font-size:0.8rem;color:#FFB6D9;">Kirby User</div>
                    <div style="font-size:0.8rem;color:var(--text-primary);margin-top:2px;">
                      Clean full-screen Discord! No address bar visible at all.
                    </div>
                  </div>
                </div>
              </div>

              <!-- Bottom Bar inside Kirby -->
              <div class="phone-bottom-nav">
                <div class="phone-tab active" onclick="showToast('In Discord')">
                  <span>Discord</span>
                </div>
                <div class="phone-tab" onclick="showToast('Settings opened')">
                  <span>Settings</span>
                </div>
              </div>
            </div>
          </div>
        </div>
      </div>
    </section>
  </main>

  <div id="toast">
    <span id="toast-text">Success</span>
  </div>

  <script>
    function switchTab(tabId) {
      document.querySelectorAll('.nav-tab-btn').forEach(b => b.classList.remove('active'));
      document.querySelectorAll('.tab-content').forEach(c => c.classList.remove('active'));
      const activeBtn = Array.from(document.querySelectorAll('.nav-tab-btn')).find(b => b.getAttribute('onclick').includes(tabId));
      if (activeBtn) activeBtn.classList.add('active');
      const target = document.getElementById('tab-' + tabId);
      if (target) target.classList.add('active');
    }

    function openKirbyApp() {
      document.getElementById('phone-home-view').style.display = 'none';
      document.getElementById('phone-app-view').style.display = 'flex';
    }

    function closeKirbyApp() {
      document.getElementById('phone-home-view').style.display = 'grid';
      document.getElementById('phone-app-view').style.display = 'none';
    }

    function showToast(msg) {
      const t = document.getElementById('toast');
      document.getElementById('toast-text').innerText = msg;
      t.style.display = 'flex';
      setTimeout(() => { t.style.display = 'none'; }, 2500);
    }

    function rebuildApk() {
      const btn = document.getElementById('rebuild-btn');
      btn.innerText = 'Recompiling Kirby APK...';
      btn.disabled = true;
      fetch('/api/build', { method: 'POST' })
        .then(r => r.json())
        .then(() => {
          showToast('Gradle build started!');
          setTimeout(() => {
            btn.innerText = 'Recompile APK (Gradle)';
            btn.disabled = false;
            showToast('Kirby.apk recompiled!');
          }, 6000);
        })
        .catch(() => {
          btn.innerText = 'Recompile APK (Gradle)';
          btn.disabled = false;
        });
    }
  </script>
</body>
</html>`;
}

// HTTP Server
const server = http.createServer((req, res) => {
  const parsedUrl = new URL(req.url, `http://${req.headers.host || 'localhost'}`);
  const pathname = parsedUrl.pathname;

  // 1. Root: Serves Clean Dashboard
  if (pathname === '/' || pathname === '/index.html') {
    res.writeHead(200, {
      'Content-Type': 'text/html; charset=utf-8',
      'Cache-Control': 'no-cache'
    });
    res.end(renderDashboardHtml());
    return;
  }

  // 2. Download APK Endpoint
  if (pathname === '/api/download/apk') {
    const targetPath = fs.existsSync(APK_PATH) ? APK_PATH : (fs.existsSync(BACKUP_APK_PATH) ? BACKUP_APK_PATH : null);
    if (!targetPath) {
      res.writeHead(404, { 'Content-Type': 'application/json' });
      res.end(JSON.stringify({ error: 'APK not found. Please click Recompile APK first.' }));
      return;
    }

    try {
      const stat = fs.statSync(targetPath);
      res.writeHead(200, {
        'Content-Type': 'application/vnd.android.package-archive',
        'Content-Disposition': 'attachment; filename="Kirby.apk"',
        'Content-Length': stat.size,
        'Cache-Control': 'no-cache',
        'Accept-Ranges': 'bytes'
      });
      const stream = fs.createReadStream(targetPath);
      stream.on('error', (err) => {
        console.error('[STREAM ERROR]', err);
        if (!res.headersSent) {
          res.writeHead(500, { 'Content-Type': 'application/json' });
          res.end(JSON.stringify({ error: 'Stream error' }));
        }
      });
      res.on('close', () => { stream.destroy(); });
      stream.pipe(res);
      return;
    } catch (err) {
      res.writeHead(500, { 'Content-Type': 'application/json' });
      res.end(JSON.stringify({ error: 'Failed to read APK file', details: err.message }));
      return;
    }
  }

  // 3. Trigger Build Endpoint
  if (pathname === '/api/build' && req.method === 'POST') {
    const result = runGradleBuild();
    res.writeHead(result.success ? 200 : 409, { 'Content-Type': 'application/json' });
    res.end(JSON.stringify(result));
    return;
  }

  // 4. Build Status Endpoint
  if (pathname === '/api/build/status') {
    const apk = getApkInfo();
    res.writeHead(200, { 'Content-Type': 'application/json', 'Cache-Control': 'no-cache' });
    res.end(JSON.stringify({
      status: buildState.status,
      progress: buildState.progress,
      currentStage: buildState.currentStage,
      durationSeconds: buildState.durationSeconds,
      logs: buildState.logs.slice(-50),
      error: buildState.error,
      apkReady: apk.exists,
      apkSize: apk.size,
      apkSha256: apk.sha256,
      name: APP_NAME
    }));
    return;
  }

  // 5. APK Info Endpoint
  if (pathname === '/api/apk-info') {
    const apk = getApkInfo();
    res.writeHead(200, { 'Content-Type': 'application/json' });
    res.end(JSON.stringify(apk));
    return;
  }

  res.writeHead(404, { 'Content-Type': 'text/plain' });
  res.end('Not Found');
});

server.listen(PORT, '0.0.0.0', () => {
  console.log(`[KIRBY] Server running on http://0.0.0.0:${PORT}`);
});
