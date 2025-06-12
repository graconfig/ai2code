sap.ui.define([
    "sap/ui/core/format/DateFormat"
], function (DateFormat) {
    "use strict";
    return {
        setState: function (v) {
            if (v === "S") {
                return "Success";
            }
            if (v === "E") {
                return "Error";
            }
            return "None";
        },

        setResult: function (v, sButtonHeader) {
            if (sButtonHeader !== "CHECK") {
                if (v === "S") {
                    return "Success";
                }
                if (v === "E") {
                    return "Error";
                }
            }
            return "";
        },

        // 0000/00/00
        date: function (value) {
            if (value) {
                var oDateFormat = DateFormat.getDateTimeInstance({
                    pattern: "yyyy/MM/dd"
                });
                return oDateFormat.format(new Date(value));
            }
            return value;
        },

        // 00:00:00
        time: function (value) {
            if (value) {
                var timeFormat = DateFormat.getTimeInstance({
                    pattern: "HH:mm:ss"
                });
                return timeFormat.format(new Date());
            }
            return value;
        },

        odataDate: function (sDate) {
            var oDate = new Date(sDate);
            var oDateFormat = sap.ui.core.format.DateFormat.getDateTimeInstance({
                pattern: "yyyy-MM-dd"
            });
            var sFormatDate = oDateFormat.format(oDate, false);
            return new Date(sFormatDate);
        },

        // ADD BEGIN BY SHIN073 2021/05/11
        checkflagReturn: function (sFlag) {
            var boolean = true;
            if (sFlag !== null && sFlag !== undefined) {
                if (sFlag.trimLeft().substr(0, 1) === "X" || sFlag.trimLeft().substr(0, 1) === "Y" || sFlag.trimLeft().substr(0, 1) === "Z") {
                    boolean = false;
                }
            }
            return boolean;
        },

        formatNumber: function (n) {
            if (n) {
                var sign = "";
                if (typeof n === "string") {
                    var bNegative = n.endsWith("-");
                    if (bNegative) {
                        n = "-" + n.substring(0, n.length - 1);
                    }
                }
                var num = Number(n);
                if (num < 0) {
                    num = num * -1;
                    sign = "-";
                }
                var re = /\d{1,3}(?=(\d{3})+$)/g;
                var n1 = num.toString().replace(/^(\d+)((\.\d+)?)$/, function (s, s1, s2) {
                    return s1.replace(re, "$&,") + s2;
                });
                if (sign === "-") {
                    n1 = sign + n1;
                }
                return n1;
            }
        },

        formatFloat: function (n) {
            if (n) {
                var sign = "";
                if (typeof n === "string") {
                    var bNegative = n.endsWith("-");
                    if (bNegative) {
                        n = "-" + n.substring(0, n.length - 1);
                    }
                }
                var num = Number(n).toFixed(2);
                if (num < 0) {
                    num = num.substring(1);
                    sign = "-";
                }
                var re = /\d{1,3}(?=(\d{3})+$)/g;
                var n1 = num.toString().replace(/^(\d+)((\.\d+)?)$/, function (s, s1, s2) {
                    return s1.replace(re, "$&,") + s2;
                });
                if (sign === "-") {
                    n1 = sign + n1;
                }
                return n1;
            }
        },
        // ADD END BY SHIN073 2021/05/11
        // ADD BEGIN BY SHIN073 2021/05/24
        convertDateToString: function (date) {
            var sString = "";
            if (date === null) {
                return sString;
            } else {
                var year = date.getFullYear();
                var month = date.getMonth() + 1;
                var day = date.getDate();
                if (day < 10) {
                    day = "0" + day;
                }
                return year + "/" + month + "/" + day;
            }
        },
        senderIcon: function (sender) {
            return sender === "assistant" ? "sap-icon://tnt/robot" : "sap-icon://tnt/user";
        },
        itemIsVisibleInList: function (id) {
            return !!id;
        }
        // ADD END BY SHIN073 2021/05/24
    };
});