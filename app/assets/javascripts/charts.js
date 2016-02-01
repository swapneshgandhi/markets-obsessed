var _now = new Date(),
    _chart;

function createUrl(url, qs) {
    if(!qs) { return url; }

    var params = Object.keys(qs);
    if(params.length) {
        url = url + '?' + params.map(function(p) {
            return p +'='+ encodeURIComponent(qs[p]);
        }).join('&');
    }

    return url;
}

function getMean(items, getItemNumber) {
    getItemNumber = getItemNumber || function(x) { return x; };

    var len = items.length,
        sum = 0;

    var i = len;
    while (i--) {
        sum = sum + getItemNumber(items[i]);
    }

    var mean = sum/len;
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
    //console.log(ohlcData);

    var ohlcSeries = {
        name: name,
        data: ohlcData,

        type: 'candlestick',
        //http://stackoverflow.com/questions/9849806/data-grouping-into-weekly-monthly-by-user
        dataGrouping: { enabled: false },
        tooltip:      { valueDecimals: 2 },
    };
    _chart.addSeries(ohlcSeries);


    //Bollinger bands:
    //http://bl.ocks.org/godds/6550889
    var bandsData = [];
    var period = 20;
    var stdDevs = 2;
    for (var i = period - 1, len = ohlcData.length; i < len; i++) {
        var slice = ohlcData.slice(i + 1 - period , i+1);

        var mean = getMean(slice, function(d) { return d[4]; });

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
        dataGrouping: { enabled: false },
        tooltip:      { valueDecimals: 2 },
        fillOpacity: 0.1,
    };
//    _chart.addSeries(bandsSeries);

    //console.log(JSON.stringify(ohlcData));
    //console.log(JSON.stringify(bandsData));
}


$(function () {
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
    $.get(url, function (data) {
        //document.write('<pre>'+data+'</pre>');
        renderYahoo(ticker, data);
    });
});

$(document).ready(function(){

$('#usEquities').css({'background-image': 'linear-gradient(to top,  #2E2E28 0%, #4D4C48 100%)'});

});


function getRandomArbitrary(min, max) {
    return Math.random() * (max - min) + min;
}

$(document).ready(function() {
  $.ajaxSetup({ cache: false }); // This part addresses an IE bug.  without it, IE will only load the first number and will never refresh
  setInterval(function() {
    $("#dollar").find(".panel").find(".subheader")[0].innerText=getRandomArbitrary(80,85)
  }, 1000); // the "3000"
});

