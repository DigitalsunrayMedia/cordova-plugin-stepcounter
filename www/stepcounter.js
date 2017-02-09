/*
    Copyright 2015 Jarrod Linahan <jarrod@texh.net>

    Permission is hereby granted, free of charge, to any person obtaining
    a copy of this software and associated documentation files (the
    "Software"), to deal in the Software without restriction, including
    without limitation the rights to use, copy, modify, merge, publish,
    distribute, sublicense, and/or sell copies of the Software, and to
    permit persons to whom the Software is furnished to do so, subject to
    the following conditions:

    The above copyright notice and this permission notice shall be
    included in all copies or substantial portions of the Software.

    THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND,
    EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF
    MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND
    NONINFRINGEMENT. IN NO EVENT SHALL THE AUTHORS OR COPYRIGHT HOLDERS BE
    LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY, WHETHER IN AN ACTION
    OF CONTRACT, TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN CONNECTION
    WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.

*/

module.exports = {
    //ACTION_CONFIGURE       : "configure",
    ACTION_START           : "start",
    ACTION_STOP            : "stop",
    ACTION_GET_STEPS       : "get_step_count",
    ACTION_TODAY_GET_STEPS : "get_today_step_count",
    ACTION_CAN_COUNT_STEPS : "can_count_steps",
    ACTION_GET_HISTORY     : "get_history",

    start: function (offset, successCallback, errorCallback) {
        offset = parseInt(offset) || 0;
        cordova.exec(successCallback, errorCallback, "CordovaStepCounter", "start", [offset]);
    },
 
    stop: function ( successCallback, errorCallback) {
        cordova.exec(successCallback, errorCallback, "CordovaStepCounter", "stop", []);
    },

    getTodayStepCount: function (successCallback, errorCallback) {
        cordova.exec(successCallback, errorCallback, "CordovaStepCounter", "get_today_step_count", []);
    },

    getStepCount: function (successCallback, errorCallback) {
        cordova.exec(successCallback, errorCallback, "CordovaStepCounter", "get_step_count", []);
    },
 
    deviceCanCountSteps: function (successCallback, errorCallback) {
        cordova.exec(function(res) {successCallback(!!res);}, errorCallback, "CordovaStepCounter", "can_count_steps", []);
    },

    getHistory: function (successCallback, errorCallback) {
        cordova.exec(function(result){
          var parsedResult = JSON.parse(result);  
          successCallback(parsedResult);
        },errorCallback, "CordovaStepCounter", "get_history", []);
    }
};
