const globals = require('globals');
module.exports = [
  { ignores: ['node_modules/**', 'dist/**', 'runtime/**', 'jre*/**'] },
  {
    files: ['**/*.js', '**/*.cjs'],
    languageOptions: {
      ecmaVersion: 'latest',
      globals: { ...globals.browser, ...globals.node },
    },
    rules: {
      'no-undef': 'error',
      'no-unreachable': 'error',
      'no-constant-condition': 'error',
    },
  },
];
