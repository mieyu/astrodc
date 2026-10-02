const { defineConfig } = require('@vue/cli-service')
const CopyPlugin = require('copy-webpack-plugin')
const path = require('path')
module.exports = defineConfig({
  transpileDependencies: true,
  chainWebpack(config) {
    // PDF.js ships its own browser build; Babel's Vue 2 preset cannot transform its private methods.
    config.module.rule('js').exclude.add(/pdfjs-dist/)
  },
  configureWebpack: {
    plugins: [new CopyPlugin({ patterns: ['cmaps', 'standard_fonts', 'wasm'].map(folder => ({
      from: path.join(path.dirname(require.resolve('pdfjs-dist/package.json')), folder),
      to: `pdf-assets/${folder}`
    })) })]
  }
})
