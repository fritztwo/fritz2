(function () {
    const rule = config.module.rules.find(r => r.test && r.test.toString().includes('css'));
    if (rule) {
        rule.use.push({
            loader: 'postcss-loader',
            options: {
                postcssOptions: {
                    plugins: [require("@tailwindcss/postcss")]
                }
            }
        });
    }
})();