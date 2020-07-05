var exec = require('cordova/exec');

exports.setupBluetooth = function (arg0, success, error) {
    exec(success, error, 'sewooklp', 'setupBluetooth', [arg0]);
};

exports.listPairedDevices = function (arg0, success, error) {
    exec(success, error, 'sewooklp', 'listPairedDevices', [arg0]);
};

exports.connectToDevice = function (arg0, success, error) {
    exec(success, error, 'sewooklp', 'connectToDevice', [arg0]);
};
