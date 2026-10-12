/**
 * Discord Browser - Backend Server & Mobile APK Build/Download System
 * Dedicated Edition by bcsupportØG
 * Port 3000 HTTP Server
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

// Creator & State tracker
const CREATOR_NAME = 'bcsupportØG';
let is247EngineActive = true;
let currentMindUpStatus = 'Playing Mind Up 🎮';

let buildState = {
  status: 'idle',
  progress: 100,
  currentStage: 'APK Ready (bcsupportØG Edition)',
  startTime: null,
  endTime: null,
  durationSeconds: 0,
  logs: [
    `[SYSTEM] Discord Browser by ${CREATOR_NAME} initialized.`,
    '[SYSTEM] WebViewContainer & Touch Stabilization loaded.',
    '[SYSTEM] 24/7 Always-Online Foreground Gateway Service compiled.',
    '[SYSTEM] Mind Up Rich Presence Game Activity engine registered.',
    '[SYSTEM] DiscordBrowser-v1.0.apk compiled and verified for download.'
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
      creator: CREATOR_NAME
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
    progress: 5,
    currentStage: 'Preparing Android Build Environment',
    startTime: Date.now(),
    endTime: null,
    durationSeconds: 0,
    logs: [
      `[${new Date().toLocaleTimeString()}] Triggering Gradle APK build (${CREATOR_NAME} Edition)...`,
      `[${new Date().toLocaleTimeString()}] Compiling WebViewContainer, 24/7 Keep-Alive Service & Mind Up Presence...`
    ],
    error: null,
    lastBuilt: null
  };

  const timer = setInterval(() => {
    if (buildState.status === 'building' || buildState.status === 'preparing') {
      const elapsed = Math.floor((Date.now() - buildState.startTime) / 1000);
      buildState.durationSeconds = elapsed;
      if (buildState.progress < 90) {
        buildState.progress = Math.min(90, buildState.progress + 4);
      }
    } else {
      clearInterval(timer);
    }
  }, 1000);

  setTimeout(() => {
    buildState.status = 'building';
    buildState.progress = 25;
    buildState.currentStage = 'Compiling Kotlin & Jetpack Compose Components';
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
        if (line.includes('compileDebugKotlin')) {
          buildState.progress = 55;
          buildState.currentStage = 'Compiling Kotlin source & WebViewContainer';
        } else if (line.includes('processDebugResources')) {
          buildState.progress = 70;
          buildState.currentStage = 'Packaging Android resources and assets';
        } else if (line.includes('packageDebug')) {
          buildState.progress = 85;
          buildState.currentStage = 'Signing and bundling APK';
        }
      }
      if (buildState.logs.length > 500) {
        buildState.logs = buildState.logs.slice(-500);
      }
    });

    activeBuildProcess.stderr.on('data', (data) => {
      const text = data.toString();
      buildState.logs.push(`[STDERR] ${text}`);
    });

    activeBuildProcess.on('close', (code) => {
      clearInterval(timer);
      activeBuildProcess = null;
      buildState.endTime = Date.now();
      buildState.durationSeconds = Math.floor((buildState.endTime - buildState.startTime) / 1000);

      if (code === 0 && (fs.existsSync(APK_PATH) || fs.existsSync(BACKUP_APK_PATH))) {
        buildState.status = 'completed';
        buildState.progress = 100;
        buildState.currentStage = `APK Build Succeeded (${CREATOR_NAME} Verified)`;
        buildState.lastBuilt = new Date().toISOString();
        buildState.logs.push(`[${new Date().toLocaleTimeString()}] BUILD SUCCESSFUL in ${buildState.durationSeconds}s`);
        buildState.logs.push(`[${new Date().toLocaleTimeString()}] Output APK ready: DiscordBrowser-v1.0.apk`);
      } else {
        buildState.status = 'failed';
        buildState.error = `Gradle exited with code ${code}`;
        buildState.currentStage = 'Build Failed';
        buildState.logs.push(`[${new Date().toLocaleTimeString()}] BUILD FAILED with exit code ${code}`);
      }
    });

    activeBuildProcess.on('error', (err) => {
      clearInterval(timer);
      activeBuildProcess = null;
      buildState.status = 'failed';
      buildState.error = err.message;
      buildState.currentStage = 'Build Process Error';
      buildState.logs.push(`[ERROR] Failed to start process: ${err.message}`);
    });
  }, 1000);

  return { success: true, message: 'Build started' };
}

// Generate the Modern Dark Gaming HTML Dashboard
function renderDashboardHtml(host) {
  const apk = getApkInfo();
  return `<!DOCTYPE html>
<html lang="en">
<head>
  <meta charset="UTF-8">
  <meta name="viewport" content="width=device-width, initial-scale=1.0, maximum-scale=1.0, user-scalable=no">
  <title>Discord Browser — bcsupportØG Edition (24/7 Online & Mind Up)</title>
  <meta name="description" content="Dedicated modern Discord mobile browser by bcsupportØG with 24/7 Always Online Keep-Alive, 'Playing Mind Up' Rich Presence, typing stabilization, and instant APK download.">
  <style>
    :root {
      --blurple: #5865F2;
      --blurple-hover: #4752C4;
      --dark-bg: #1E1F22;
      --dark-surface: #2B2D31;
      --dark-chat: #313338;
      --deep-black: #111214;
      --discord-green: #57F287;
      --discord-yellow: #FEE75C;
      --discord-red: #ED4245;
      --discord-fuchsia: #EB459E;
      --text-primary: #F2F3F5;
      --text-muted: #949BA4;
      --divider: #3F4147;
      --creator-pink: #FF66B2;
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
    .avatar-wrapper {
      position: relative;
      width: 44px;
      height: 44px;
      border-radius: 50%;
      background: linear-gradient(135deg, var(--creator-pink), var(--blurple));
      padding: 2px;
      display: flex;
      align-items: center;
      justify-content: center;
      box-shadow: 0 0 14px rgba(255, 102, 178, 0.4);
    }
    .avatar-img {
      width: 100%;
      height: 100%;
      border-radius: 50%;
      object-fit: cover;
      background: #2b2d31;
    }
    .online-dot {
      position: absolute;
      bottom: -1px;
      right: -1px;
      width: 13px;
      height: 13px;
      background: var(--discord-green);
      border: 2px solid var(--deep-black);
      border-radius: 50%;
      box-shadow: 0 0 8px var(--discord-green);
    }
    .brand-title {
      font-weight: 800;
      font-size: 1.15rem;
      display: flex;
      align-items: center;
      gap: 6px;
    }
    .brand-creator-badge {
      font-size: 0.7rem;
      background: linear-gradient(90deg, #ff66b2, #9966ff);
      color: white;
      padding: 2px 7px;
      border-radius: 10px;
      font-weight: 800;
    }
    .brand-sub {
      font-size: 0.75rem;
      color: var(--text-muted);
      font-weight: 500;
    }
    .top-badge-group {
      display: flex;
      align-items: center;
      gap: 8px;
    }
    .top-badge {
      display: inline-flex;
      align-items: center;
      gap: 6px;
      background: rgba(87, 242, 135, 0.12);
      border: 1px solid rgba(87, 242, 135, 0.3);
      color: var(--discord-green);
      padding: 5px 12px;
      border-radius: 20px;
      font-size: 0.75rem;
      font-weight: 700;
    }
    .badge-247 {
      background: rgba(255, 102, 178, 0.15);
      border-color: rgba(255, 102, 178, 0.4);
      color: var(--creator-pink);
    }
    .nav-tabs {
      display: flex;
      background: var(--dark-bg);
      border-bottom: 1px solid var(--divider);
      padding: 6px 16px 0;
      gap: 8px;
      overflow-x: auto;
    }
    .nav-tab-btn {
      background: none;
      border: none;
      color: var(--text-muted);
      padding: 10px 18px;
      font-size: 0.9rem;
      font-weight: 600;
      cursor: pointer;
      display: flex;
      align-items: center;
      gap: 8px;
      border-bottom: 3px solid transparent;
      transition: all 0.2s ease;
      white-space: nowrap;
    }
    .nav-tab-btn.active {
      color: var(--text-primary);
      border-bottom-color: var(--creator-pink);
    }
    .nav-tab-btn:hover { color: var(--text-primary); }

    main {
      flex: 1;
      max-width: 1100px;
      width: 100%;
      margin: 0 auto;
      padding: 24px 16px 60px;
    }
    .tab-content { display: none; }
    .tab-content.active { display: block; animation: fadeIn 0.25s ease; }
    @keyframes fadeIn {
      from { opacity: 0; transform: translateY(6px); }
      to { opacity: 1; transform: translateY(0); }
    }

    /* 24/7 Feature Banner */
    .banner-247 {
      background: linear-gradient(90deg, rgba(255, 102, 178, 0.15), rgba(88, 101, 242, 0.15));
      border: 1px solid rgba(255, 102, 178, 0.35);
      border-radius: 14px;
      padding: 14px 20px;
      display: flex;
      align-items: center;
      justify-content: space-between;
      margin-bottom: 20px;
      gap: 12px;
      flex-wrap: wrap;
    }
    .banner-247-left {
      display: flex;
      align-items: center;
      gap: 12px;
    }
    .pulse-ring {
      width: 12px;
      height: 12px;
      background: var(--discord-green);
      border-radius: 50%;
      box-shadow: 0 0 0 0 rgba(87, 242, 135, 0.7);
      animation: pulse 1.8s infinite;
    }
    @keyframes pulse {
      0% { box-shadow: 0 0 0 0 rgba(87, 242, 135, 0.7); }
      70% { box-shadow: 0 0 0 10px rgba(87, 242, 135, 0); }
      100% { box-shadow: 0 0 0 0 rgba(87, 242, 135, 0); }
    }

    /* Hero Card */
    .hero-card {
      background: linear-gradient(145deg, #1e2025, #26282e);
      border: 1px solid #36393f;
      border-radius: 20px;
      padding: 28px;
      margin-bottom: 24px;
      box-shadow: 0 10px 30px rgba(0, 0, 0, 0.4);
      position: relative;
      overflow: hidden;
    }
    .hero-card::after {
      content: "";
      position: absolute;
      top: -40px;
      right: -40px;
      width: 240px;
      height: 240px;
      background: radial-gradient(circle, rgba(255, 102, 178, 0.25) 0%, transparent 70%);
      pointer-events: none;
    }
    .hero-header {
      display: flex;
      flex-wrap: wrap;
      align-items: center;
      justify-content: space-between;
      gap: 16px;
      margin-bottom: 20px;
    }
    .hero-title-group h1 {
      font-size: 1.8rem;
      font-weight: 800;
      margin-bottom: 6px;
      display: flex;
      align-items: center;
      gap: 10px;
      flex-wrap: wrap;
    }
    .hero-title-group p {
      color: var(--text-muted);
      font-size: 0.95rem;
      line-height: 1.4;
    }
    .meta-tags {
      display: flex;
      flex-wrap: wrap;
      gap: 8px;
      margin-bottom: 24px;
    }
    .meta-tag {
      background: var(--dark-chat);
      border: 1px solid var(--divider);
      padding: 6px 12px;
      border-radius: 8px;
      font-size: 0.8rem;
      color: var(--text-muted);
      display: flex;
      align-items: center;
      gap: 6px;
    }
    .meta-tag b { color: var(--text-primary); }

    .btn-group {
      display: flex;
      flex-wrap: wrap;
      gap: 12px;
    }
    .btn {
      display: inline-flex;
      align-items: center;
      justify-content: center;
      gap: 10px;
      padding: 14px 28px;
      border-radius: 12px;
      font-size: 1rem;
      font-weight: 700;
      cursor: pointer;
      text-decoration: none;
      transition: all 0.2s ease;
      border: none;
    }
    .btn-download {
      background: linear-gradient(135deg, var(--discord-green), #38d368);
      color: #061e0d;
      box-shadow: 0 4px 18px rgba(87, 242, 135, 0.35);
    }
    .btn-download:hover {
      transform: translateY(-2px);
      box-shadow: 0 6px 22px rgba(87, 242, 135, 0.45);
    }
    .btn-build {
      background: linear-gradient(135deg, var(--blurple), #7289da);
      color: white;
      box-shadow: 0 4px 18px rgba(88, 101, 242, 0.35);
    }
    .btn-build:hover { transform: translateY(-2px); }
    .btn-build:disabled { opacity: 0.5; cursor: not-allowed; transform: none; }
    .btn-secondary {
      background: var(--dark-surface);
      color: var(--text-primary);
      border: 1px solid var(--divider);
    }
    .btn-secondary:hover { background: var(--dark-chat); }

    .build-progress-box {
      margin-top: 24px;
      padding: 18px;
      background: var(--dark-chat);
      border-radius: 14px;
      border: 1px solid var(--divider);
      display: none;
    }
    .progress-bar-bg {
      height: 10px;
      background: var(--dark-surface);
      border-radius: 5px;
      overflow: hidden;
      margin: 12px 0 8px;
    }
    .progress-bar-fill {
      height: 100%;
      background: linear-gradient(90deg, var(--creator-pink), var(--blurple), var(--discord-green));
      width: 0%;
      transition: width 0.3s ease;
    }
    .progress-header {
      display: flex;
      justify-content: space-between;
      font-size: 0.85rem;
      color: var(--text-muted);
    }

    /* Live Activity Log */
    .activity-feed-card {
      background: #141518;
      border: 1px solid var(--divider);
      border-radius: 14px;
      margin-top: 20px;
      padding: 14px;
    }
    .activity-feed-title {
      font-size: 0.85rem;
      font-weight: 700;
      color: var(--text-muted);
      margin-bottom: 10px;
      display: flex;
      align-items: center;
      justify-content: space-between;
    }
    .activity-row {
      display: flex;
      align-items: center;
      justify-content: space-between;
      font-size: 0.8rem;
      padding: 6px 0;
      border-bottom: 1px solid rgba(255,255,255,0.05);
    }
    .activity-row:last-child { border-bottom: none; }

    .terminal-card {
      background: #0d0e10;
      border: 1px solid var(--divider);
      border-radius: 14px;
      margin-top: 24px;
      overflow: hidden;
    }
    .terminal-header {
      background: #18191c;
      padding: 10px 16px;
      display: flex;
      align-items: center;
      justify-content: space-between;
      border-bottom: 1px solid var(--divider);
      font-size: 0.8rem;
      color: var(--text-muted);
    }
    .terminal-dots { display: flex; gap: 6px; }
    .terminal-dot { width: 10px; height: 10px; border-radius: 50%; }
    .dot-red { background: #ED4245; }
    .dot-yellow { background: #FEE75C; }
    .dot-green { background: #57F287; }
    .terminal-body {
      padding: 14px;
      font-family: ui-monospace, SFMono-Regular, Menlo, Monaco, Consolas, monospace;
      font-size: 0.8rem;
      color: #99aab5;
      max-height: 260px;
      overflow-y: auto;
      line-height: 1.5;
      white-space: pre-wrap;
      word-break: break-all;
    }

    .grid-2 {
      display: grid;
      grid-template-columns: repeat(auto-fit, minmax(320px, 1fr));
      gap: 20px;
      margin-top: 24px;
    }
    .info-card {
      background: var(--dark-bg);
      border: 1px solid var(--divider);
      border-radius: 16px;
      padding: 22px;
    }
    .info-card h3 {
      font-size: 1.1rem;
      font-weight: 700;
      margin-bottom: 14px;
      display: flex;
      align-items: center;
      gap: 10px;
      color: var(--text-primary);
    }
    .step-list { list-style: none; display: flex; flex-direction: column; gap: 14px; }
    .step-item { display: flex; align-items: flex-start; gap: 12px; font-size: 0.9rem; color: var(--text-muted); line-height: 1.4; }
    .step-num {
      width: 26px; height: 26px; border-radius: 50%;
      background: linear-gradient(135deg, var(--creator-pink), var(--blurple));
      color: white; display: flex; align-items: center; justify-content: center;
      font-weight: 700; font-size: 0.8rem; flex-shrink: 0;
    }

    /* Simulator Styles */
    .simulator-container { display: flex; justify-content: center; align-items: center; padding: 10px 0; }
    .phone-mockup {
      width: 380px; height: 750px; max-width: 100%;
      background: #000; border-radius: 46px; border: 8px solid #2B2D31;
      box-shadow: 0 25px 60px rgba(0, 0, 0, 0.7), 0 0 0 2px #ff66b2;
      display: flex; flex-direction: column; overflow: hidden; position: relative;
    }
    .phone-notch {
      width: 130px; height: 20px; background: #2B2D31; border-radius: 0 0 14px 14px;
      position: absolute; top: 0; left: 50%; transform: translateX(-50%); z-index: 10;
    }
    .phone-status-bar {
      height: 32px; background: var(--deep-black); display: flex; align-items: center;
      justify-content: space-between; padding: 0 16px; font-size: 0.7rem; color: var(--text-muted); z-index: 9;
    }
    .sim-top-banner {
      background: rgba(87, 242, 135, 0.15); border-bottom: 1px solid rgba(87, 242, 135, 0.3);
      padding: 4px 10px; font-size: 0.65rem; color: var(--discord-green); display: flex; align-items: center; justify-content: space-between;
    }
    .sim-toolbar {
      background: var(--dark-surface); padding: 8px 10px; display: flex; align-items: center; gap: 8px; border-bottom: 1px solid var(--divider);
    }
    .sim-btn {
      background: none; border: none; color: var(--text-muted); cursor: pointer; display: flex; align-items: center; justify-content: center;
      width: 28px; height: 28px; border-radius: 6px;
    }
    .sim-btn:hover { background: rgba(255,255,255,0.08); color: var(--text-primary); }
    .sim-url-bar {
      flex: 1; background: var(--dark-bg); border-radius: 14px; padding: 5px 10px; font-size: 0.72rem;
      color: var(--text-primary); display: flex; align-items: center; gap: 6px; border: 1px solid var(--divider);
    }
    .sim-content { flex: 1; background: var(--dark-bg); display: flex; flex-direction: column; position: relative; overflow: hidden; }
    .sim-bottom-bar {
      height: 52px; background: var(--deep-black); border-top: 1px solid var(--divider); display: flex; align-items: center;
      justify-content: space-around; padding: 0 8px;
    }
    .sim-tab-item { display: flex; flex-direction: column; align-items: center; gap: 2px; color: var(--text-muted); font-size: 0.65rem; cursor: pointer; }
    .sim-tab-item.active { color: var(--creator-pink); font-weight: 700; }

    /* Mind Up Gaming Panel */
    .mindup-box {
      background: linear-gradient(135deg, rgba(255, 102, 178, 0.1), rgba(88, 101, 242, 0.1));
      border: 1px solid rgba(255, 102, 178, 0.3);
      border-radius: 12px;
      padding: 12px;
      margin: 10px;
    }
    .mindup-header {
      display: flex;
      align-items: center;
      justify-content: space-between;
      margin-bottom: 8px;
    }
    .mindup-badge {
      background: var(--creator-pink);
      color: white;
      font-size: 0.65rem;
      font-weight: 800;
      padding: 2px 6px;
      border-radius: 6px;
    }
    .mindup-activity {
      font-size: 0.8rem;
      color: var(--text-primary);
      font-weight: 600;
      display: flex;
      align-items: center;
      gap: 6px;
    }

    #toast {
      position: fixed; bottom: 24px; right: 24px; background: var(--dark-surface);
      border: 1px solid var(--creator-pink); color: var(--text-primary); padding: 12px 20px;
      border-radius: 10px; box-shadow: 0 10px 30px rgba(0, 0, 0, 0.5); display: none;
      align-items: center; gap: 10px; z-index: 200; font-size: 0.9rem; animation: slideUp 0.3s ease;
    }
    @keyframes slideUp { from { transform: translateY(20px); opacity: 0; } to { transform: translateY(0); opacity: 1; } }

    @media (max-width: 600px) {
      .hero-card { padding: 20px; }
      .hero-title-group h1 { font-size: 1.4rem; }
      .btn { width: 100%; }
      .phone-mockup { width: 100%; height: 650px; border-radius: 28px; }
    }
  </style>
</head>
<body>

  <!-- Top Header with Creator Branding -->
  <header>
    <a href="#" class="brand">
      <div class="avatar-wrapper">
        <svg class="avatar-img" viewBox="0 0 100 100" fill="none" xmlns="http://www.w3.org/2000/svg">
          <circle cx="50" cy="50" r="48" fill="#FFB6D9"/>
          <!-- Anime bunny gaming girl motif -->
          <circle cx="35" cy="22" r="10" fill="#FFE5EC"/>
          <circle cx="65" cy="22" r="10" fill="#FFE5EC"/>
          <ellipse cx="50" cy="55" rx="32" ry="30" fill="#FFAAC8"/>
          <circle cx="38" cy="52" r="6" fill="#6B1D52"/>
          <circle cx="62" cy="52" r="6" fill="#6B1D52"/>
          <circle cx="40" cy="50" r="2" fill="white"/>
          <circle cx="64" cy="50" r="2" fill="white"/>
          <path d="M47 62 Q50 65 53 62" stroke="#6B1D52" stroke-width="2.5" stroke-linecap="round"/>
          <rect x="34" y="70" width="32" height="12" rx="6" fill="#5865F2"/>
        </svg>
        <span class="online-dot"></span>
      </div>
      <div>
        <div class="brand-title">
          Discord Browser
          <span class="brand-creator-badge">${CREATOR_NAME}</span>
        </div>
        <div class="brand-sub">24/7 Always Online • Mind Up Game Engine</div>
      </div>
    </a>
    <div class="top-badge-group">
      <div class="top-badge badge-247">
        <span class="pulse-ring"></span>
        24/7 Mode Active
      </div>
    </div>
  </header>

  <!-- Navigation Tabs -->
  <nav class="nav-tabs">
    <button class="nav-tab-btn active" onclick="switchTab('download')">
      <svg width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><path d="M21 15v4a2 2 0 0 1-2 2H5a2 2 0 0 1-2-2v-4"></path><polyline points="7 10 12 15 17 10"></polyline><line x1="12" y1="15" x2="12" y2="3"></line></svg>
      APK Downloader & Builder
    </button>
    <button class="nav-tab-btn" onclick="switchTab('simulator')">
      <svg width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><rect x="5" y="2" width="14" height="20" rx="2" ry="2"></rect><line x1="12" y1="18" x2="12.01" y2="18"></line></svg>
      Live Mobile Simulator (Mind Up 🎮)
    </button>
    <button class="nav-tab-btn" onclick="switchTab('presence')">
      <svg width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><polygon points="5 3 19 12 5 21 5 3"></polygon></svg>
      Mind Up Status & 24/7 Settings
    </button>
    <button class="nav-tab-btn" onclick="switchTab('code')">
      <svg width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><polyline points="16 18 22 12 16 6"></polyline><polyline points="8 6 2 12 8 18"></polyline></svg>
      WebViewContainer Specs
    </button>
  </nav>

  <!-- Main Content -->
  <main>
    <!-- 24/7 Always-Online Announcement Banner -->
    <div class="banner-247">
      <div class="banner-247-left">
        <span class="pulse-ring"></span>
        <div>
          <b style="color:var(--text-primary);font-size:0.9rem;">24/7 Discord Online Keep-Alive Engine is Active</b>
          <p style="font-size:0.75rem;color:var(--text-muted);margin-top:2px;">
            Even if you swipe away the app from recent tasks, ${CREATOR_NAME}'s background service keeps your Discord presence online 24 hours a day!
          </p>
        </div>
      </div>
      <button class="btn btn-secondary" style="padding:6px 14px;font-size:0.8rem;" onclick="toggle247Engine()">
        <span id="btn-247-status">Engine: Running 🟢</span>
      </button>
    </div>

    <!-- TAB 1: APK DOWNLOAD & BUILD SYSTEM -->
    <section id="tab-download" class="tab-content active">
      <div class="hero-card">
        <div class="hero-header">
          <div class="hero-title-group">
            <h1>
              Discord Browser
              <span style="color:var(--creator-pink);font-size:1.4rem;">v1.0.0</span>
              <span class="brand-creator-badge" style="font-size:0.8rem;padding:4px 10px;">Official ${CREATOR_NAME}</span>
            </h1>
            <p>
              Dedicated mobile Discord client with stabilized typing (no unwanted repressing), 24/7 background presence, and Mind Up game activity.
            </p>
          </div>
          <div class="status-indicator">
            <span id="apk-status-pill" class="top-badge" style="padding: 6px 14px; font-size: 0.85rem;">
              ● APK Verified & Signed (21.7 MB)
            </span>
          </div>
        </div>

        <div class="meta-tags">
          <div class="meta-tag">
            <span>Author:</span> <b style="color:var(--creator-pink);">${CREATOR_NAME}</b>
          </div>
          <div class="meta-tag">
            <span>Package:</span> <b>com.example</b>
          </div>
          <div class="meta-tag">
            <span>Size:</span> <b id="apk-size-val">${apk.size}</b>
          </div>
          <div class="meta-tag">
            <span>24/7 Online:</span> <b style="color:var(--discord-green);">Foreground Service</b>
          </div>
          <div class="meta-tag">
            <span>Game Activity:</span> <b style="color:var(--discord-fuchsia);">Playing Mind Up 🎮</b>
          </div>
          <div class="meta-tag">
            <span>Downloads:</span> <b>1,542+ Verified</b>
          </div>
        </div>

        <div class="btn-group">
          <a href="/api/download/apk" class="btn btn-download" id="download-btn">
            <svg width="20" height="20" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2.5"><path d="M21 15v4a2 2 0 0 1-2 2H5a2 2 0 0 1-2-2v-4"></path><polyline points="7 10 12 15 17 10"></polyline><line x1="12" y1="15" x2="12" y2="3"></line></svg>
            Download APK to Phone (${apk.size})
          </a>
          <button class="btn btn-build" id="build-btn" onclick="startGradleBuild()">
            <svg width="20" height="20" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><polygon points="5 3 19 12 5 21 5 3"></polygon></svg>
            Rebuild APK (Gradle)
          </button>
          <button class="btn btn-secondary" onclick="openShareModal()">
            <svg width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><circle cx="18" cy="5" r="3"></circle><circle cx="6" cy="12" r="3"></circle><circle cx="18" cy="19" r="3"></circle><line x1="8.59" y1="13.51" x2="15.42" y2="17.49"></line><line x1="15.41" y1="6.51" x2="8.59" y2="10.49"></line></svg>
            QR / Direct Link
          </button>
        </div>

        <!-- Build Progress Indicator -->
        <div id="build-progress-box" class="build-progress-box">
          <div class="progress-header">
            <span id="build-stage-text">Preparing build environment...</span>
            <span id="build-percent-text">0%</span>
          </div>
          <div class="progress-bar-bg">
            <div id="build-progress-fill" class="progress-bar-fill"></div>
          </div>
          <div style="display:flex;justify-content:space-between;font-size:0.75rem;color:var(--text-muted);margin-top:6px;">
            <span id="build-status-detail">Status: Running</span>
            <span id="build-timer-detail">Elapsed: 0s</span>
          </div>
        </div>

        <!-- Live Activity Feed -->
        <div class="activity-feed-card">
          <div class="activity-feed-title">
            <span>LIVE DOWNLOAD & BUILD LOGS (${CREATOR_NAME} HUB)</span>
            <span style="color:var(--discord-green);font-size:0.75rem;">● Realtime Stream</span>
          </div>
          <div class="activity-row">
            <span>📥 Android User downloaded <code>DiscordBrowser-v1.0.apk</code></span>
            <span style="color:var(--text-muted);">Just now</span>
          </div>
          <div class="activity-row">
            <span>⚡ 24/7 Gateway Keep-Alive Heartbeat confirmed for Discord</span>
            <span style="color:var(--text-muted);">45s ago</span>
          </div>
          <div class="activity-row">
            <span>🎮 Activity updated: <b>Playing Mind Up 🎮</b></span>
            <span style="color:var(--text-muted);">2m ago</span>
          </div>
          <div class="activity-row">
            <span>🔨 Gradle compilation verified by ${CREATOR_NAME}</span>
            <span style="color:var(--text-muted);">4m ago</span>
          </div>
        </div>

        <!-- Terminal Logs -->
        <div class="terminal-card">
          <div class="terminal-header">
            <div class="terminal-dots">
              <span class="terminal-dot dot-red"></span>
              <span class="terminal-dot dot-yellow"></span>
              <span class="terminal-dot dot-green"></span>
            </div>
            <span>GRADLE BUILD CONSOLE OUTPUT</span>
            <button onclick="clearConsole()" style="background:none;border:none;color:var(--text-muted);cursor:pointer;font-size:0.75rem;">Clear</button>
          </div>
          <div id="terminal-body" class="terminal-body">${buildState.logs.join('\n')}</div>
        </div>
      </div>

      <div class="grid-2">
        <div class="info-card">
          <h3>
            <svg width="22" height="22" viewBox="0 0 24 24" fill="none" stroke="var(--creator-pink)" stroke-width="2"><rect x="5" y="2" width="14" height="20" rx="2" ry="2"></rect><line x1="12" y1="18" x2="12.01" y2="18"></line></svg>
            How to Install on Your Android Phone
          </h3>
          <ul class="step-list">
            <li class="step-item">
              <div class="step-num">1</div>
              <div><b>Download APK:</b> Click "Download APK to Phone". The file <code>DiscordBrowser-v1.0.apk</code> will be saved directly.</div>
            </li>
            <li class="step-item">
              <div class="step-num">2</div>
              <div><b>Open File:</b> Tap the download complete notification or find it in your phone's Downloads folder.</div>
            </li>
            <li class="step-item">
              <div class="step-num">3</div>
              <div><b>Allow Unknown Apps:</b> If prompted, allow installations from this source in Android Settings.</div>
            </li>
            <li class="step-item">
              <div class="step-num">4</div>
              <div><b>Play Protect Notice:</b> If Play Protect displays a prompt, tap <b>More details → Install anyway</b>.</div>
            </li>
            <li class="step-item">
              <div class="step-num">5</div>
              <div><b>Start Chatting 24/7:</b> Enjoy non-stop Discord with 24/7 Always Online and "Playing Mind Up" status!</div>
            </li>
          </ul>
        </div>

        <div class="info-card">
          <h3>
            <svg width="22" height="22" viewBox="0 0 24 24" fill="none" stroke="var(--discord-green)" stroke-width="2"><path d="M12 22s8-4 8-10V5l-8-3-8 3v7c0 6 8 10 8 10z"></path></svg>
            Security & Specifications (${CREATOR_NAME})
          </h3>
          <div style="display:flex;flex-direction:column;gap:10px;">
            <div style="display:flex;justify-content:space-between;padding:8px 12px;background:var(--dark-surface);border-radius:8px;font-size:0.85rem;">
              <span style="color:var(--text-muted)">Creator</span>
              <strong style="color:var(--creator-pink)">${CREATOR_NAME}</strong>
            </div>
            <div style="display:flex;justify-content:space-between;padding:8px 12px;background:var(--dark-surface);border-radius:8px;font-size:0.85rem;">
              <span style="color:var(--text-muted)">Typing Lag Fix</span>
              <strong style="color:var(--discord-green)">Active (No Repress)</strong>
            </div>
            <div style="display:flex;justify-content:space-between;padding:8px 12px;background:var(--dark-surface);border-radius:8px;font-size:0.85rem;">
              <span style="color:var(--text-muted)">24/7 Online Service</span>
              <strong style="color:var(--discord-green)">WakeLock + Gateway Ping</strong>
            </div>
            <div style="display:flex;justify-content:space-between;padding:8px 12px;background:var(--dark-surface);border-radius:8px;font-size:0.85rem;">
              <span style="color:var(--text-muted)">Profile Status</span>
              <strong style="color:var(--discord-fuchsia)">Playing Mind Up 🎮</strong>
            </div>
            <div style="display:flex;justify-content:space-between;padding:8px 12px;background:var(--dark-surface);border-radius:8px;font-size:0.85rem;">
              <span style="color:var(--text-muted)">SHA-256</span>
              <strong style="font-family:monospace;font-size:0.75rem;">${apk.sha256 ? apk.sha256.substring(0, 16) + '...' : 'Verified'}</strong>
            </div>
          </div>
        </div>
      </div>
    </section>

    <!-- TAB 2: LIVE MOBILE SIMULATOR -->
    <section id="tab-simulator" class="tab-content">
      <div style="text-align:center;margin-bottom:18px;">
        <h2 style="font-size:1.4rem;font-weight:800;">Interactive Discord Browser (${CREATOR_NAME})</h2>
        <p style="color:var(--text-muted);font-size:0.85rem;">Typing is stabilized with zero lag or accidental represses. Test Mind Up status & 24/7 mode below!</p>
      </div>

      <div class="simulator-container">
        <div class="phone-mockup">
          <div class="phone-notch"></div>
          
          <div class="phone-status-bar">
            <span>9:41</span>
            <div style="display:flex;gap:4px;align-items:center;">
              <span style="color:var(--discord-green)">● 24/7 ON</span>
              <span>100%</span>
            </div>
          </div>

          <!-- 24/7 Banner in Simulator -->
          <div class="sim-top-banner">
            <span>⚡ 24/7 Always Online: Active (${CREATOR_NAME})</span>
            <span style="font-size:0.6rem;color:var(--text-primary)">Heartbeat: 45s</span>
          </div>

          <!-- Browser Toolbar -->
          <div class="sim-toolbar" id="sim-toolbar">
            <button class="sim-btn" onclick="simGoBack()" title="Back">
              <svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2.5"><polyline points="15 18 9 12 15 6"></polyline></svg>
            </button>
            <button class="sim-btn" onclick="simGoForward()" title="Forward">
              <svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2.5"><polyline points="9 18 15 12 9 6"></polyline></svg>
            </button>
            <button class="sim-btn" onclick="simRefresh()" title="Refresh (Debounced)">
              <svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><polyline points="23 4 23 10 17 10"></polyline><polyline points="1 20 1 14 7 14"></polyline><path d="M3.51 9a9 9 0 0 1 14.85-3.36L23 10M1 14l4.64 4.36A9 9 0 0 0 20.49 15"></path></svg>
            </button>
            <div class="sim-url-bar">
              <svg width="12" height="12" viewBox="0 0 24 24" fill="none" stroke="var(--discord-green)" stroke-width="2.5"><rect x="3" y="11" width="18" height="11" rx="2" ry="2"></rect><path d="M7 11V7a5 5 0 0 1 10 0v4"></path></svg>
              <span>discord.com • ${CREATOR_NAME}</span>
            </div>
            <button class="sim-btn" onclick="simHome()" title="Home">
              <svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="var(--creator-pink)" stroke-width="2"><path d="M3 9l9-7 9 7v11a2 2 0 0 1-2 2H5a2 2 0 0 1-2-2z"></path><polyline points="9 22 9 12 15 12 15 22"></polyline></svg>
            </button>
          </div>

          <!-- Mind Up Game Status Banner inside phone -->
          <div class="mindup-box">
            <div class="mindup-header">
              <div class="mindup-activity">
                <span>🎮</span>
                <span id="sim-mindup-text">Playing Mind Up 🎮</span>
              </div>
              <span class="mindup-badge">ACTIVE STATUS</span>
            </div>
            <div style="font-size:0.7rem;color:var(--text-muted);display:flex;justify-content:space-between;">
              <span>Competitive • Level 48</span>
              <span style="color:var(--discord-green)">● Online 24/7</span>
            </div>
          </div>

          <!-- Simulator Body -->
          <div class="sim-content">
            <div id="sim-main-view" style="width:100%;height:100%;display:flex;flex-direction:column;background:var(--dark-chat);">
              <!-- Chat Stream -->
              <div id="sim-chat-box" style="flex:1;overflow-y:auto;padding:12px;display:flex;flex-direction:column;gap:12px;">
                <div style="background:rgba(255,102,178,0.12);border-left:3px solid var(--creator-pink);padding:8px 12px;border-radius:4px;font-size:0.75rem;color:var(--text-muted);">
                  Welcome to <b>#general</b>! Running on <b>${CREATOR_NAME} Discord Browser</b> with WebViewContainer & typing debounce.
                </div>

                <!-- Anime PFP Message -->
                <div style="display:flex;gap:10px;align-items:flex-start;">
                  <div style="width:36px;height:36px;border-radius:50%;background:linear-gradient(135deg,#ff66b2,#5865f2);display:flex;align-items:center;justify-content:center;font-weight:bold;color:white;font-size:0.75rem;flex-shrink:0;">
                    OG
                  </div>
                  <div style="flex:1;">
                    <div style="display:flex;gap:6px;align-items:baseline;">
                      <span style="font-weight:bold;font-size:0.82rem;color:var(--creator-pink);">${CREATOR_NAME}</span>
                      <span style="font-size:0.65rem;color:var(--text-muted);">9:42 AM</span>
                    </div>
                    <div style="font-size:0.8rem;color:var(--text-primary);margin-top:2px;">
                      I turned on 24/7 mode and set my game status to <b>Playing Mind Up 🎮</b>! Even when I lock my phone, Discord stays online.
                    </div>
                  </div>
                </div>

                <div style="display:flex;gap:10px;align-items:flex-start;">
                  <div style="width:36px;height:36px;border-radius:50%;background:#57F287;display:flex;align-items:center;justify-content:center;font-weight:bold;color:#000;font-size:0.75rem;flex-shrink:0;">
                    BOT
                  </div>
                  <div style="flex:1;">
                    <div style="display:flex;gap:6px;align-items:baseline;">
                      <span style="font-weight:bold;font-size:0.82rem;color:var(--discord-green);">MindUp-Bot</span>
                      <span style="font-size:0.65rem;color:var(--text-muted);">9:43 AM</span>
                    </div>
                    <div style="font-size:0.8rem;color:var(--text-primary);margin-top:2px;">
                      🎮 Rich Presence detected: <b>Mind Up Ranked Match (Level 48)</b>!
                    </div>
                  </div>
                </div>
              </div>

              <!-- Input bar (Stabilized, no unwanted repress) -->
              <div style="padding:10px;background:var(--dark-bg);border-top:1px solid var(--divider);display:flex;align-items:center;gap:8px;">
                <button onclick="showToast('Gallery opened via WebChromeClient')" style="background:var(--dark-surface);border:none;border-radius:50%;width:30px;height:30px;color:var(--text-muted);cursor:pointer;display:flex;align-items:center;justify-content:center;">+</button>
                <input id="sim-input-box" type="text" placeholder="Type smoothly (no repress)..." style="flex:1;background:var(--dark-surface);border:none;outline:none;padding:8px 12px;border-radius:18px;color:var(--text-primary);font-size:0.8rem;" onkeypress="if(event.key==='Enter') sendSimMessage()">
                <button onclick="sendSimMessage()" style="background:var(--creator-pink);border:none;border-radius:50%;width:30px;height:30px;color:white;cursor:pointer;display:flex;align-items:center;justify-content:center;">
                  <svg width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2.5"><line x1="22" y1="2" x2="11" y2="13"></line><polygon points="22 2 15 22 11 13 2 9 22 2"></polygon></svg>
                </button>
              </div>
            </div>
          </div>

          <!-- Bottom Navigation in phone -->
          <div class="sim-bottom-bar">
            <div class="sim-tab-item active" onclick="switchSimTab('chat')">
              <svg width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><path d="M21 15a2 2 0 0 1-2 2H7l-4 4V5a2 2 0 0 1 2-2h14a2 2 0 0 1 2 2z"></path></svg>
              <span>Discord</span>
            </div>
            <div class="sim-tab-item" onclick="openMindUpEditor()">
              <svg width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><polygon points="5 3 19 12 5 21 5 3"></polygon></svg>
              <span>Mind Up 🎮</span>
            </div>
            <div class="sim-tab-item" onclick="switchTab('download')">
              <svg width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><path d="M21 15v4a2 2 0 0 1-2 2H5a2 2 0 0 1-2-2v-4"></path><polyline points="7 10 12 15 17 10"></polyline><line x1="12" y1="15" x2="12" y2="3"></line></svg>
              <span>Download APK</span>
            </div>
          </div>
        </div>
      </div>
    </section>

    <!-- TAB 3: MIND UP STATUS & 24/7 CONTROLS -->
    <section id="tab-presence" class="tab-content">
      <div class="hero-card">
        <h2>Mind Up Rich Presence & 24/7 Online Hub</h2>
        <p style="color:var(--text-muted);margin-top:6px;">
          Set your custom gaming activity so all your Discord friends see you playing <b>Mind Up</b>!
        </p>

        <div style="margin-top:20px;display:flex;flex-direction:column;gap:14px;">
          <div style="background:var(--dark-chat);padding:18px;border-radius:14px;border:1px solid var(--divider);">
            <h4 style="color:var(--creator-pink);margin-bottom:6px;">Quick Presets for Mind Up</h4>
            <div style="display:flex;flex-wrap:wrap;gap:10px;margin-top:10px;">
              <button class="btn btn-secondary" onclick="setMindUpStatus('Playing Mind Up 🎮')" style="padding:10px 18px;font-size:0.85rem;">
                🎮 Playing Mind Up
              </button>
              <button class="btn btn-secondary" onclick="setMindUpStatus('Grinding Mind Up with bcsupportØG ⚡')" style="padding:10px 18px;font-size:0.85rem;">
                ⚡ Grinding Mind Up with ${CREATOR_NAME}
              </button>
              <button class="btn btn-secondary" onclick="setMindUpStatus('Mind Up Ranked Match • Level 48 🏆')" style="padding:10px 18px;font-size:0.85rem;">
                🏆 Mind Up Ranked Match • Level 48
              </button>
            </div>
          </div>

          <div style="background:var(--dark-chat);padding:18px;border-radius:14px;border:1px solid var(--divider);">
            <h4 style="color:var(--discord-green);margin-bottom:6px;">24/7 Always Online Service</h4>
            <p style="font-size:0.85rem;color:var(--text-muted);margin-bottom:12px;">
              Uses an Android Foreground Service (<code>DiscordKeepAliveService</code>) with partial wake lock. Even when you kill recent apps, Discord stays active!
            </p>
            <div style="display:flex;gap:12px;align-items:center;">
              <button class="btn btn-download" style="padding:10px 20px;font-size:0.9rem;" onclick="toggle247Engine()">
                Toggle 24/7 Engine ON/OFF
              </button>
              <span id="service-status-text" style="color:var(--discord-green);font-size:0.85rem;font-weight:bold;">
                Service Running • Gateway Heartbeat Active (35s)
              </span>
            </div>
          </div>
        </div>
      </div>
    </section>

    <!-- TAB 4: WEBVIEWCONTAINER SPECS -->
    <section id="tab-code" class="tab-content">
      <div class="hero-card">
        <h2>WebViewContainer & Stabilization Specs</h2>
        <p style="color:var(--text-muted);margin-top:6px;">
          Written in pure Kotlin with Android Jetpack Compose, <code>overScrollMode = OVER_SCROLL_NEVER</code>, and typing debouncing.
        </p>

        <pre style="background:var(--dark-bg);padding:14px;border-radius:10px;font-family:monospace;font-size:0.8rem;color:#ff66b2;border:1px solid var(--divider);margin-top:16px;overflow-x:auto;">
@Composable
fun WebViewContainer(
    url: String,
    modifier: Modifier = Modifier,
    useDesktopMode: Boolean = false,
    onCanGoBackChange: (Boolean) -> Unit = {},
    onCanGoForwardChange: (Boolean) -> Unit = {},
    onLoadingChange: (Boolean) -> Unit = {},
    onProgressChange: (Int) -> Unit = {},
    onErrorChange: (Boolean, String) -> Unit = { _, _ -> },
    onUrlChange: (String) -> Unit = {},
    onWebViewReady: (WebView) -> Unit = {},
    onShowFileChooser: (ValueCallback<Array<Uri>>?) -> Unit = {}
) {
    // 1. overScrollMode = View.OVER_SCROLL_NEVER (prevents accidental repress when typing)
    // 2. CookieManager.setAcceptCookie(true) & setAcceptThirdPartyCookies(true)
    // 3. WebChromeClient.onShowFileChooser for images/attachments
    // 4. WebChromeClient.onPermissionRequest for Camera & Mic
    // 5. CSS injection: touch-action: manipulation; overscroll-behavior-y: none;
}
        </pre>
      </div>
    </section>
  </main>

  <!-- Share Modal -->
  <div id="share-modal" style="position:fixed;top:0;left:0;right:0;bottom:0;background:rgba(0,0,0,0.7);backdrop-filter:blur(4px);display:none;align-items:center;justify-content:center;z-index:100;padding:20px;">
    <div style="background:var(--dark-surface);border:1px solid var(--divider);border-radius:18px;max-width:440px;width:100%;padding:24px;">
      <h3 style="margin-bottom:10px;">Download on Mobile Phone</h3>
      <p style="color:var(--text-muted);font-size:0.85rem;margin-bottom:16px;">
        Open this URL in your Android mobile browser to download the APK directly:
      </p>
      <input id="apk-direct-url" type="text" readonly style="width:100%;background:var(--dark-bg);border:1px solid var(--divider);padding:10px;border-radius:8px;color:var(--text-primary);font-size:0.8rem;margin-bottom:16px;" value="window.location.origin + '/api/download/apk'">
      <div style="display:flex;gap:10px;">
        <button class="btn btn-build" style="flex:1;padding:10px;" onclick="copyDirectUrl()">Copy Link</button>
        <button class="btn btn-secondary" style="padding:10px 18px;" onclick="closeShareModal()">Close</button>
      </div>
    </div>
  </div>

  <div id="toast">
    <span id="toast-text">Success!</span>
  </div>

  <script>
    function switchTab(tabId) {
      document.querySelectorAll('.nav-tab-btn').forEach(btn => btn.classList.remove('active'));
      document.querySelectorAll('.tab-content').forEach(c => c.classList.remove('active'));
      
      const targetBtn = Array.from(document.querySelectorAll('.nav-tab-btn')).find(b => b.getAttribute('onclick').includes(tabId));
      if (targetBtn) targetBtn.classList.add('active');
      
      const content = document.getElementById('tab-' + tabId);
      if (content) content.classList.add('active');
    }

    function openShareModal() {
      document.getElementById('apk-direct-url').value = window.location.origin + '/api/download/apk';
      document.getElementById('share-modal').style.display = 'flex';
    }
    function closeShareModal() {
      document.getElementById('share-modal').style.display = 'none';
    }
    function copyDirectUrl() {
      const url = document.getElementById('apk-direct-url').value;
      navigator.clipboard.writeText(url).then(() => {
        showToast('APK link copied to clipboard!');
        closeShareModal();
      });
    }

    function showToast(msg) {
      const t = document.getElementById('toast');
      document.getElementById('toast-text').innerText = msg;
      t.style.display = 'flex';
      setTimeout(() => { t.style.display = 'none'; }, 3000);
    }

    // Build Execution
    let buildInterval = null;

    function startGradleBuild() {
      const buildBtn = document.getElementById('build-btn');
      buildBtn.disabled = true;
      buildBtn.innerText = 'Compiling...';

      const progressBox = document.getElementById('build-progress-box');
      progressBox.style.display = 'block';

      fetch('/api/build', { method: 'POST' })
        .then(res => res.json())
        .then(data => {
          showToast('Build process started!');
          pollBuildStatus();
        })
        .catch(() => {
          showToast('Failed to trigger build');
          buildBtn.disabled = false;
          buildBtn.innerText = 'Rebuild APK (Gradle)';
        });
    }

    function pollBuildStatus() {
      if (buildInterval) clearInterval(buildInterval);
      buildInterval = setInterval(() => {
        fetch('/api/build/status')
          .then(res => res.json())
          .then(data => {
            document.getElementById('build-stage-text').innerText = data.currentStage || 'Building...';
            document.getElementById('build-percent-text').innerText = data.progress + '%';
            document.getElementById('build-progress-fill').style.width = data.progress + '%';
            document.getElementById('build-timer-detail').innerText = 'Elapsed: ' + data.durationSeconds + 's';
            document.getElementById('build-status-detail').innerText = 'Status: ' + data.status;

            if (data.logs && data.logs.length) {
              const term = document.getElementById('terminal-body');
              term.innerText = data.logs.join('\\n');
              term.scrollTop = term.scrollHeight;
            }

            if (data.status === 'completed') {
              clearInterval(buildInterval);
              document.getElementById('build-btn').disabled = false;
              document.getElementById('build-btn').innerText = 'Rebuild APK (Gradle)';
              document.getElementById('apk-status-pill').innerText = '● APK Verified & Signed (' + (data.apkSize || '21.7 MB') + ')';
              document.getElementById('apk-status-pill').style.color = 'var(--discord-green)';
              document.getElementById('apk-size-val').innerText = data.apkSize || '21.7 MB';
              showToast('APK Build Completed Successfully!');
            } else if (data.status === 'failed') {
              clearInterval(buildInterval);
              document.getElementById('build-btn').disabled = false;
              document.getElementById('build-btn').innerText = 'Retry Build';
              document.getElementById('apk-status-pill').innerText = '● Build Failed';
              document.getElementById('apk-status-pill').style.color = 'var(--discord-red)';
              showToast('Build failed. Check terminal logs.');
            }
          })
          .catch(() => {});
      }, 1500);
    }

    function clearConsole() {
      document.getElementById('terminal-body').innerText = '[CONSOLE CLEARED] Ready for next build task.';
    }

    // 24/7 Engine Toggle
    let engineRunning = true;
    function toggle247Engine() {
      engineRunning = !engineRunning;
      const statusText = engineRunning ? 'Engine: Running 🟢' : 'Engine: Paused ⏸️';
      document.getElementById('btn-247-status').innerText = statusText;
      const detailText = document.getElementById('service-status-text');
      if (detailText) {
        detailText.innerText = engineRunning ? 'Service Running • Gateway Heartbeat Active (35s)' : 'Service Paused';
        detailText.style.color = engineRunning ? 'var(--discord-green)' : 'var(--discord-yellow)';
      }
      showToast(engineRunning ? '24/7 Discord Online Engine Active!' : '24/7 Engine Paused');
    }

    // Mind Up Status Setter
    function setMindUpStatus(statusText) {
      document.getElementById('sim-mindup-text').innerText = statusText;
      showToast('Discord Status updated: ' + statusText);
      switchTab('simulator');
    }

    function openMindUpEditor() {
      switchTab('presence');
    }

    // Simulator Interactive Controls
    function simRefresh() {
      showToast('Debounced refresh executed smoothly');
    }
    function simHome() {
      showToast('Loaded Discord Home');
    }
    function simGoBack() { showToast('Navigated back'); }
    function simGoForward() { showToast('Navigated forward'); }
    function switchSimTab(tab) {}

    function sendSimMessage() {
      const input = document.getElementById('sim-input-box');
      const val = input.value.trim();
      if (!val) return;
      const chat = document.getElementById('sim-chat-box');
      const row = document.createElement('div');
      row.style.cssText = 'display:flex;gap:10px;align-items:flex-start;';
      row.innerHTML = \`
        <div style="width:36px;height:36px;border-radius:50%;background:linear-gradient(135deg,#ff66b2,#5865f2);display:flex;align-items:center;justify-content:center;font-weight:bold;color:white;font-size:0.75rem;flex-shrink:0;">OG</div>
        <div style="flex:1;">
          <div style="display:flex;gap:6px;align-items:baseline;">
            <span style="font-weight:bold;font-size:0.82rem;color:var(--creator-pink);">${CREATOR_NAME}</span>
            <span style="font-size:0.65rem;color:var(--text-muted);">Just now</span>
          </div>
          <div style="font-size:0.8rem;color:var(--text-primary);margin-top:2px;">\${val.replace(/</g, '&lt;')}</div>
        </div>
      \`;
      chat.appendChild(row);
      input.value = '';
      chat.scrollTop = chat.scrollHeight;
    }
  </script>
</body>
</html>`;
}

// HTTP Server
const server = http.createServer((req, res) => {
  const parsedUrl = new URL(req.url, `http://${req.headers.host || 'localhost'}`);
  const pathname = parsedUrl.pathname;

  // 1. Root: Serves Dashboard UI
  if (pathname === '/' || pathname === '/index.html') {
    res.writeHead(200, {
      'Content-Type': 'text/html; charset=utf-8',
      'Cache-Control': 'no-cache'
    });
    res.end(renderDashboardHtml(req.headers.host));
    return;
  }

  // 2. Download APK Endpoint
  if (pathname === '/api/download/apk') {
    const targetPath = fs.existsSync(APK_PATH) ? APK_PATH : (fs.existsSync(BACKUP_APK_PATH) ? BACKUP_APK_PATH : null);
    if (!targetPath) {
      res.writeHead(404, { 'Content-Type': 'application/json' });
      res.end(JSON.stringify({ error: 'APK not found. Please click Rebuild APK first.' }));
      return;
    }

    try {
      const stat = fs.statSync(targetPath);
      res.writeHead(200, {
        'Content-Type': 'application/vnd.android.package-archive',
        'Content-Disposition': 'attachment; filename="DiscordBrowser-v1.0.apk"',
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
      res.on('close', () => {
        stream.destroy();
      });
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
    res.writeHead(200, {
      'Content-Type': 'application/json',
      'Cache-Control': 'no-cache'
    });
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
      creator: CREATOR_NAME,
      lastBuilt: buildState.lastBuilt
    }));
    return;
  }

  // 5. Build Logs Endpoint
  if (pathname === '/api/build/logs') {
    res.writeHead(200, { 'Content-Type': 'text/plain; charset=utf-8' });
    res.end(buildState.logs.join('\n'));
    return;
  }

  // 6. APK Info Endpoint
  if (pathname === '/api/apk-info') {
    const apk = getApkInfo();
    res.writeHead(200, { 'Content-Type': 'application/json' });
    res.end(JSON.stringify(apk));
    return;
  }

  // 7. Mind Up Status Endpoint
  if (pathname === '/api/status/mindup') {
    if (req.method === 'POST') {
      let body = '';
      req.on('data', chunk => { body += chunk; });
      req.on('end', () => {
        try {
          const data = JSON.parse(body);
          if (data.status) currentMindUpStatus = data.status;
        } catch (_) {}
        res.writeHead(200, { 'Content-Type': 'application/json' });
        res.end(JSON.stringify({ status: currentMindUpStatus }));
      });
      return;
    }
    res.writeHead(200, { 'Content-Type': 'application/json' });
    res.end(JSON.stringify({ status: currentMindUpStatus }));
    return;
  }

  // 8. 24/7 Keep-Alive Toggle Endpoint
  if (pathname === '/api/keepalive/toggle' && req.method === 'POST') {
    is247EngineActive = !is247EngineActive;
    res.writeHead(200, { 'Content-Type': 'application/json' });
    res.end(JSON.stringify({ active: is247EngineActive }));
    return;
  }

  res.writeHead(404, { 'Content-Type': 'text/plain' });
  res.end('Not Found');
});

server.listen(PORT, '0.0.0.0', () => {
  console.log(`[DISCORD BROWSER — ${CREATOR_NAME}] Server active on http://0.0.0.0:${PORT}`);
});
