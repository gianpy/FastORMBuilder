const fs = require('fs');
const path = require('path');
const { transformSync } = require('@babel/core');

const webviewDir = path.join(__dirname, 'src/main/resources/webview');
const htmlPath = path.join(webviewDir, 'fastbuilder.html');
const html = fs.readFileSync(htmlPath, 'utf8');

// Extract the JSX code from <script type="text/babel">...</script>
const regex = /<script type="text\/babel">([\s\S]*?)<\/script>/;
const match = html.match(regex);

if (!match) {
    console.error('No <script type="text/babel"> found in HTML');
    process.exit(1);
}

const jsxCode = match[1];
console.log(`Extracted ${jsxCode.length} chars of JSX`);

// Transpile JSX to plain JS
const result = transformSync(jsxCode, {
    presets: ['@babel/preset-react'],
    filename: 'app.jsx',
});

console.log(`Transpiled to ${result.code.length} chars of JS`);

// Write the compiled JS
const compiledPath = path.join(webviewDir, 'app.compiled.js');
fs.writeFileSync(compiledPath, result.code);

// Replace the babel script tag with a regular script tag referencing the compiled file
const newHtml = html
    .replace('<script src="babel.min.js"></script>\n', '') // Remove babel
    .replace(regex, '<script src="app.compiled.js"></script>');

fs.writeFileSync(htmlPath, newHtml);
console.log('Done! HTML updated to use pre-compiled JS (no more Babel runtime).');
