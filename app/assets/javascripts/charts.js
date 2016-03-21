var _now = new Date(),
    _chart;

function createUrl(url, qs) {
    if (!qs) {
        return url;
    }

    var params = Object.keys(qs);
    if (params.length) {
        url = url + '?' + params.map(function(p) {
            return p + '=' + encodeURIComponent(qs[p]);
        }).join('&');
    }

    return url;
}

function getMean(items, getItemNumber) {
    getItemNumber = getItemNumber || function(x) {
        return x;
    };

    var len = items.length,
        sum = 0;

    var i = len;
    while (i--) {
        sum = sum + getItemNumber(items[i]);
    }

    var mean = sum / len;
    return mean;
}

function createUrlYahoo(ticker, from, to) {
    //"Historical Prices" -> "Set Date Range" -> "Get Prices"..
    //  http://finance.yahoo.com/q/hp?s=MSFT&a=00&b=1&c=2015&d=11&e=31&f=2015&g=d
    //.. -> "Download to Spreadsheet":
    //  http://real-chart.finance.yahoo.com/table.csv?s=MSFT&a=00&b=1&c=2015&d=11&e=31&f=2015&g=d&ignore=.csv

    var urlBase = 'http://real-chart.finance.yahoo.com/table.csv';
    var qs = {};

    function qsDate(paramNames, date) {
        //Yahoo format: Month (zero based) *before* day, and then year:
        //http://stackoverflow.com/questions/2013255/how-to-get-year-month-day-from-a-date-object
        var dateParts = [date.getUTCMonth(), date.getUTCDate(), date.getUTCFullYear()];

        dateParts.forEach(function(d, i) {
            qs[paramNames[i]] = d;
        });
    }

    qs.s = ticker;
    qsDate(['a', 'b', 'c'], from);
    qsDate(['d', 'e', 'f'], to);
    qs.g = 'd';
    qs.ignore = '.csv';

    var url = createUrl(urlBase, qs);
    return url;
}

function renderYahoo(name, data) {
    /*
    Date,Open,High,Low,Close,Volume,Adj Close
    2015-09-11,16330.400391,16434.759766,16244.650391,16433.089844,104630000,16433.089844
    2015-09-10,16252.570312,16441.939453,16212.080078,16330.400391,122690000,16330.400391
    2015-09-09,16505.039062,16664.650391,16220.099609,16253.570312,118790000,16253.570312
    2015-09-08,16109.929688,16503.410156,16109.929688,16492.679688,123870000,16492.679688
    ...
    */

    var days = data.split('\n').filter(function(row, i) {
        //Remove header row and any empty rows (there's usually one at the end):
        return (row && (i !== 0));
    });
    //console.log(days);

    //.sort(): Highcharts wants the data sorted ascending by date,
    //         and luckily each "day" row starts with the date in the sortable yyyy-mm-dd format:
    var ohlcData = days.sort()
        .map(function(day) {
            var dayInfo = day.split(',');
            return [
                //new Date('2015-08-11') => UTC (which is what we want)
                //new Date(2015, 7, 11)  => Local
                new Date(dayInfo[0]).getTime(),

                Number(dayInfo[1]),
                Number(dayInfo[2]),
                Number(dayInfo[3]),
                Number(dayInfo[4]),
            ];
        });
    console.log(ohlcData);

    var ohlcSeries = {
        name: name,
        data: ohlcData,

        type: 'candlestick',
        //http://stackoverflow.com/questions/9849806/data-grouping-into-weekly-monthly-by-user
        dataGrouping: {
            enabled: false
        },
        tooltip: {
            valueDecimals: 2
        },
    };
    _chart.addSeries(ohlcSeries);


    //Bollinger bands:
    //http://bl.ocks.org/godds/6550889
    var bandsData = [];
    var period = 20;
    var stdDevs = 2;
    for (var i = period - 1, len = ohlcData.length; i < len; i++) {
        var slice = ohlcData.slice(i + 1 - period, i + 1);

        var mean = getMean(slice, function(d) {
            return d[4];
        });

        var stdDev = Math.sqrt(getMean(slice.map(function(d) {
            return Math.pow(d[4] - mean, 2);
        })));

        bandsData.push([
            ohlcData[i][0],
            mean - (stdDevs * stdDev),
            mean + (stdDevs * stdDev)
        ]);
    }

    //http://www.highcharts.com/component/content/article/2-news/46-gauges-ranges-and-polar-charts-in-beta#ranges
    var bandsSeries = {
        name: 'Bollinger',
        data: bandsData,

        type: 'arearange',
        dataGrouping: {
            enabled: false
        },
        tooltip: {
            valueDecimals: 2
        },
        fillOpacity: 0.1,
    };
    //    _chart.addSeries(bandsSeries);

    //console.log(JSON.stringify(ohlcData));
    //console.log(JSON.stringify(bandsData));
}

//symbol, date of update, time of update
//XAUUSD=X, DX-Y.NYB
//curl -L http://finance.yahoo.com/d/quotes.csv\?e\=.csv\&f\=sl1d1t1c1p2\&s\=EURUSD\=X
//name open change low high
//curl -L http://finance.yahoo.com/d/quotes.csv\?s\=AAPL+GOOG+MSFT\&f\=nabocgh
//Date,Open,High,Low,Close,Volume,Adj Close
//curl http://real-chart.finance.yahoo.com/table.csv\?s\=MSFT\&a\=00\&b\=1\&c\=2015\&d\=11\&e\=31\&f\=2015\&g\=d\&ignore\=.csv
//Date,Open,High,Low,Close,Volume
//curl "http://www.google.com/finance/historical?q=GOOGL&output=csv"
//time, open, high, low , close
//curl -L "http://www.google.com/finance/getprices?q=TNX&i=3600&p=90d&f=d,o,h,l,c"
//understand fields by https://github.com/hongtaocai/googlefinance/blob/master/googlefinance/__init__.py
//curl http://finance.google.com/finance/info\?client\=ig\&q\=NASDAQ:GOOG
//$('#chart1 .chart').highcharts().series[0].data
//$('#chart1 .chart').highcharts().series[0]setData([Array])
$(function() {
    _chart = new Highcharts.StockChart({
        plotOptions: {
            candlestick: {
                color: '#e54444',
                upColor: '#379850'
            }
        },

        chart: {
            renderTo: document.querySelector('#chart1 .chart')
        },
        title: {
            text: 'Stock Price'
        },
        yAxis: {
            opposite: false
        },
        rangeSelector: {
            //3 months:
            selected: 1
        }
    });

    var ticker = '^DJI'
    var to = new Date(_now);
    var from = new Date(to);
    from.setMonth(to.getMonth() - 12);

    var url = 'http://crossorigin.me/' + createUrlYahoo(ticker, from, to);
    //*
    $.get(url, function(data) {
        //document.write('<pre>'+data+'</pre>');
        renderYahoo(ticker, data);
    });
});

function getRandomArbitrary(min, max) {
    return Math.random() * (max - min) + min;
}

$(document).ready(function() {
$('#title').css({
       'background-image': 'linear-gradient(to top,  #2E2E28 0%, #4D4C48 100%)'
    });
});

//
//$(document).ready(function() {
//  $.ajaxSetup({ cache: false }); // This part addresses an IE bug.  without it, IE will only load the first number and will never refresh
//  setInterval(function() {
//    $("#dollar").find(".panel").find(".subheader")[0].innerText=getRandomArbitrary(80,85)
//  }, 1000); // the "3000"
//});


var getAxisMax, getAxisMin, getChartArray, getChartOptions, getPricesFromArray, handleFlip, populateStockHistory, updateStockChart;

$(function() {
    var ws;
    ws = new WebSocket($("body").data("ws-url"));
    console.log("ws js")
    ws.onmessage = function(event) {
        var message;
        message = JSON.parse(event.data);
        switch (message.type) {
            case "quotes":
                return populateQuotes(message.quotes);
            default:
                return console.log(message);
        }
    };
});


populateQuotes = function(message) {

    for (var i = 0; i < message.length; i++) {
        var quote = message[i];
        var chart, chartHolder, detailsHolder, flipContainer, flipper, plot;

        var myElem = document.getElementById(quote.symbol);
        if (myElem === null) {

            sym = $("<div>").addClass("subheader").prop("id", quote.symbol);
            sym.innerText = quote.symbol + ": " + parseFloat(quote.currentPrice).toFixed(2); + " "+ parseFloat(quote.sentimentScore).toFixed(2)
            panel = $("#" + quote.symbol).find(".panel").append(sym)

        } else {
            $('#'+ quote.symbol).height(($('#chart1').height()/2.03))
            //console.log(parseFloat(quote.currentPrice).toFixed(2) + parseFloat(quote.openPrice).toFixed(2))
            if (parseFloat(quote.currentPrice) >= parseFloat(quote.openPrice)){
            $('#'+ quote.symbol).css({
                    'background-image': 'linear-gradient(to top,  #198c19 0%, #147014 100%)'
                });
            }
            else{
            $('#'+ quote.symbol).css({
                                'background-image': 'linear-gradient(to top,  #e50000 0%, #b70000 100%)'
            });
            }
            myElem.innerText = quote.symbol + ": " + parseFloat(quote.currentPrice).toFixed(2) + " "+ parseFloat(quote.sentimentScore).toFixed(2)
        }
        //console.log(obj.id);
    }

};