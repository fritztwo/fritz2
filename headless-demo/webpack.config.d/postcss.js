(function () {
    const rule = config.module.rules.find(r => r.test && r.test.toString().includes('css'));

    if (rule) {
        // Wir laden das Plugin, aber wir lassen die Optionen erst einmal leer
        const tailwind = require("@tailwindcss/postcss");

        rule.use.push({
            loader: 'postcss-loader',
            options: {
                postcssOptions: {
                    plugins: [
                        tailwind
                    ]
                }
            }
        });
    }
})();