var exec = require('cordova/exec');

exports.coolMethod = function (arg0, success, error) {
    exec(success, error, 'sewooklp', 'coolMethod', [arg0]);
};

exports.setupBluetooth = function (arg0, success, error) {
    exec(success, error, 'sewooklp', 'setupBluetooth', [arg0]);
};
