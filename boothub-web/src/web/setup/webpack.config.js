var path = require('path');
var resourcesRoot = path.resolve(__dirname, '../../main/resources');
var HtmlWebpackPlugin = require('html-webpack-plugin');
var ExtractTextPlugin = require('extract-text-webpack-plugin');
var CleanWebpackPlugin = require('clean-webpack-plugin');


module.exports = {
    entry: './src/index.js',
    output: {
        path: path.resolve(resourcesRoot, 'static'),
        filename: 'bundle.[hash].js'
    },
    module: {
        rules: [{
            test: /\.css$/,
            use: ExtractTextPlugin.extract({
                use: 'css-loader'
            })
        }]
    },
    plugins: [
        new CleanWebpackPlugin(['static'], {root: resourcesRoot}),
        new ExtractTextPlugin('styles.css'),
    ]
};
