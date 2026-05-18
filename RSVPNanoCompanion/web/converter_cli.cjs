#!/usr/bin/env node

const fs = require("node:fs");
const path = require("node:path");

function usage() {
  console.error("Usage: node RSVPNanoCompanion/web/converter_cli.cjs text|html <input> <output> [title]");
}

const [, , command, inputPath, outputPath, title] = process.argv;

if (!["text", "html"].includes(command) || !inputPath || !outputPath) {
  usage();
  process.exit(2);
}

(async () => {
  const { convertHtmlToRsvp, convertTextToRsvp } = await import("./converter_core.mjs");
  const content = fs.readFileSync(inputPath, "utf8");
  const filename = path.basename(inputPath);
  const output =
    command === "html"
      ? convertHtmlToRsvp({
          filename,
          title,
          source: filename,
          markup: content,
        })
      : convertTextToRsvp({
          filename,
          title,
          source: filename,
          text: content,
        });

  fs.writeFileSync(outputPath, output, "utf8");
})().catch((error) => {
  console.error(error);
  process.exit(1);
});
