var app = angular.module("mainApp", ['ui.toggle', 'ngAnimate', "ngStorage"]);
app.controller("mainCtrl", function($scope, $http, $timeout, $interval, $localStorage){
    $scope.chosenSortOption = "id";
    $scope.chosenOrderOption = "Ascending";
    $localStorage.favoriteListData = [];
    $scope.favoriteListData = $localStorage.favoriteListData;
    //$scope.favoriteListData = [];
    $scope.favoriteCount = 0;
    $localStorage.wholeFavoriteData = [];
    $scope.wholeFavoriteData = $localStorage.wholeFavoriteData;
    //$scope.wholeFavoriteData = [];
    $localStorage.allChartStringData = [];
    $scope.allChartStringData = $localStorage.allChartStringData;
    //$scope.allChartStringData = [];
    $scope.hasBeenInFavoriteList = false;

    $scope.toLeft = false;
    $scope.toRight = false;
    $scope.ngSwitchSelected = "item1";

    $scope.disableOrder = true;

    $scope.currentActive = true;
    $scope.chartActive = "price";

    $scope.toggleValue = false;
    $scope.autorefreshPromise;
    $scope.autoRefreshStatus = false;

    // for layout
    $scope.initVaribles = function(){
        $scope.showFavoriteList = true;
        $scope.showDetailDiv = false;

        $scope.currentActive = true;

        $scope.hideCurrent = false;
        $scope.hideHistory = true;
        $scope.hideNews = true;

        $scope.hidePriceProgressBar = false;
        $scope.hideSMAProgressBar = false;
        $scope.hideEMAProgressBar = false;
        $scope.hideSTOCHProgressBar = false;
        $scope.hideRSIProgressBar = false;
        $scope.hideADXProgressBar = false;
        $scope.hideCCIProgressBar = false;
        $scope.hideBBANDSProgressBar = false;
        $scope.hideMACDProgressBar = false;
        $scope.hideNewsProgressBar = true;
        $scope.hideHistoryProgressBar = true;
        $scope.hideStockDetailProgressBar = true;

        $scope.hasBeenInFavoriteList = false;

        $scope.chartActive = "price";

        $scope.detailErrorOrRegular = 'regular';
        $scope.priceErrorOrRegular = 'regular';
        $scope.SMAErrorOrRegular = 'regular';
        $scope.EMAErrorOrRegular = 'regular';
        $scope.STOCHErrorOrRegular = 'regular';
        $scope.RSIErrorOrRegular = 'regular';
        $scope.ADXErrorOrRegular = 'regular';
        $scope.CCIErrorOrRegular = 'regular';
        $scope.BBANDSErrorOrRegular = 'regular';
        $scope.MACDErrorOrRegular = 'regular';
        $scope.HistoryErrorOrRegular = 'regular';
        $scope.NewsErrorOrRegular = 'regular';

        // for data
        $scope.chartData = {};
        $scope.detailStatisticData;
        $scope.newsData;
    }

    // clear button
    $scope.clear = function(){
        $scope.symbol = "";
        $scope.inputForm.$setPristine();
        $scope.inputForm.$setUntouched();
        $scope.hideSelectList = true;
        $scope.initVaribles();
        $scope.hideCurrent = true;
        $scope.hideHistory = true;
        $scope.hideNews = true;
        $scope.disableOrder = true;
        if($scope.ngSwitchSelected == "item2"){
            $scope.changeItem();
        }
    };

    // auto complete
    $scope.complete = function(inputString, inForm) {
        if(inForm.symbol.$invalid){
            $scope.hideSelectList = true;
            return;
        }
        $scope.hideSelectList = false;
        $http({
            url : "http://127.0.0.1:3000/AutoComplete",
            method : "GET",
            params : {symbol : inputString}
        }).success(function(data, status, headers, config){
            $scope.symbolList = data.JSONdata;
            //console.log("data result : " + JSON.stringify(data.JSONdata));
            //console.log("hide status:" + $scope.hideSelectList);
            $scope.hideSelectList = false;
            var candidates = [];
            angular.forEach($scope.symbolList, function(candidate){
                var tmp = candidate["Symbol"] + " - " + candidate["Name"] + " (" + candidate["Exchange"] + ")";
                //console.log(tmp);
                candidates.push(tmp);
            });
            $scope.filterSymbol = candidates;
        })
        .error(function(data, status, headers, config){
            console.log("error");
            $scope.filterSymbol = ["APIError - please wait for one minute; do not click this msg."];
        });
    }
    $scope.fillTextbox = function(string){
        var sym = string.split(" - ")[0];
        $scope.symbol = sym;
        $scope.hideSelectList = true;
    } // end autp complete

    // quote
    $scope.quote = function(string) {
        $scope.initVaribles();
        if($scope.ngSwitchSelected == "item1"){
            $scope.changeItem();
        }
        //for(var i = 0; i < $scope.favoriteListData.length; i++){
        for(var i = 0; i < $localStorage.favoriteListData.length; i++){
            if($localStorage.favoriteListData[i].symbol == string){
                $scope.hasBeenInFavoriteList = true;
            }
        }
        $scope.hideStockDetailProgressBar = false;
        $scope.hideHistoryProgressBar = false;
        $scope.hideNewsProgressBar = false;
        $scope.hideSelectList = true;
        $scope.disableOrder = false;
        // get time seriers data
        $http({
            url : "http://127.0.0.1:3000/TimeSeries",
            method : "GET",
            params : {symbol : string}
        }).success(function(data, status, headers, config){
            //console.log(data);
            if(data.JSONdata == "APIError"){
                $scope.detailErrorOrRegular = 'error';
                $scope.priceErrorOrRegular = 'error';
                console.log("TimeSeries Error.");
                return;
            }
            $scope.chartData['price'] = data.JSONdata;
            var stat = {
                "Stock Ticker Symbol" : data.Statistic["Stock Ticker Symbol"],
                "Last Price" : data.Statistic["Last Price"],
                "Change (Change Percent)" : data.Statistic["Change"] + " (" + data.Statistic["Change Percent"] + ")",
                "Timestamp" : data.Statistic["Timestamp"],
                "Open" : data.Statistic["Open"],
                "Close" : data.Statistic["Close"],
                "Day's Range" : data.Statistic["Day's Range"],
                "Volume" : data.Statistic["Volume"]
            };
            $scope.detailStatisticData = stat;
            if(parseFloat(data.Statistic["Change"]) < 0){
                $scope.hideArrow = true;
                $scope.stockChangeColor = "red";
            } else {
                $scope.hideArrow = false;
                $scope.stockChangeColor = "green";
            }
            //draw price chart
            $scope.hidePriceProgressBar = true;
            $scope.hideStockDetailProgressBar = true;
            $scope.drawPriceChart();
        })
        .error(function(data, status, headers, config){
            $scope.detailErrorOrRegular = 'error';
            $scope.priceErrorOrRegular = 'error';
            console.log("TimeSeries Fail");
        });
        
        // get indicator data
        $http({
            url : "http://127.0.0.1:3000/Indicator",
            method : "GET",
            params : {symbol : string, indicator : "SMA"}
        }).success(function(data, status, headers, config){
            //console.log(data.JSONdata);
            if(data.JSONdata == "APIError"){
                $scope.SMAErrorOrRegular = 'error';
                console.log("SMA Error.");
                return;
            }
            $scope.chartData["SMA"] = data.JSONdata;
            $scope.chartData["SMA"]["dates"] = data.dates;
            $scope.hideSMAProgressBar = true;
            if($scope.chartActive == "SMA"){
                $scope.drawChart("SMA");
            }
            console.log("SMA has received.");
            //$scope.drawChart("SMA");
        })
        .error(function(data, status, headers, config){
            $scope.SMAErrorOrRegular = 'error';
            console.log("SMA Fail");
        });
        $http({
            url : "http://127.0.0.1:3000/Indicator",
            method : "GET",
            params : {symbol : $scope.symbol, indicator : "EMA"}
        }).success(function(data, status, headers, config){
            if(data.JSONdata == "APIError"){
                $scope.EMAErrorOrRegular = 'error';
                console.log("EMA Error.");
                return;
            }
            $scope.chartData["EMA"] = data.JSONdata;
            $scope.chartData["EMA"]["dates"] = data.dates;
            $scope.hideEMAProgressBar = true;
            if($scope.chartActive == "EMA"){
                $scope.drawChart("EMA");
            }
            console.log("EMA has received.");
        })
        .error(function(data, status, headers, config){
            $scope.EMAErrorOrRegular = 'error';
            console.log("EMA Fail.");
        });
        $http({
            url : "http://127.0.0.1:3000/Indicator",
            method : "GET",
            params : {symbol : $scope.symbol, indicator : "STOCH"}
        }).success(function(data, status, headers, config){
            if(data.JSONdata == "APIError"){
                $scope.STOCHErrorOrRegular = 'error';
                console.log("STOCH Error.");
                return;
            }
            $scope.chartData["STOCH"] = data.JSONdata;
            $scope.chartData["STOCH"]["dates"] = data.dates;
            $scope.hideSTOCHProgressBar = true;
            if($scope.chartActive == "STOCH"){
                $scope.drawChart("STOCH");
            }
            console.log("STOCH has received.");
        })
        .error(function(data, status, headers, config){
            $scope.STOCHErrorOrRegular = 'error';
            console.log("STOCH Fail.");
        });
        $http({
            url : "http://127.0.0.1:3000/Indicator",
            method : "GET",
            params : {symbol : $scope.symbol, indicator : "RSI"}
        }).success(function(data, status, headers, config){
            if(data.JSONdata == "APIError"){
                $scope.RSIErrorOrRegular = 'error';
                console.log("RSI Error.");
                return;
            }
            $scope.chartData["RSI"] = data.JSONdata;
            $scope.chartData["RSI"]["dates"] = data.dates;
            $scope.hideRSIProgressBar = true;
            if($scope.chartActive == "RSI"){
                $scope.RSIErrorOrRegular = 'error';
                $scope.drawChart("RSI");
            }
            console.log("RSI has received.");
        })
        .error(function(data, status, headers, config){
            console.log("RSI Fail.");
        });
        $http({
            url : "http://127.0.0.1:3000/Indicator",
            method : "GET",
            params : {symbol : $scope.symbol, indicator : "ADX"}
        }).success(function(data, status, headers, config){
            if(data.JSONdata == "APIError"){
                $scope.ADXErrorOrRegular = 'error';
                console.log("ADX Error.");
                return;
            }
            $scope.chartData["ADX"] = data.JSONdata;
            $scope.chartData["ADX"]["dates"] = data.dates;
            $scope.hideADXProgressBar = true;
            if($scope.chartActive == "ADX"){
                $scope.drawChart("ADX");
            }
            console.log("ADX has received.");
        })
        .error(function(data, status, headers, config){
            $scope.ADXErrorOrRegular = 'error';
            console.log("ADX Fail.");
        });
        $http({
            url : "http://127.0.0.1:3000/Indicator",
            method : "GET",
            params : {symbol : $scope.symbol, indicator : "CCI"}
        }).success(function(data, status, headers, config){
            if(data.JSONdata == "APIError"){
                $scope.CCIErrorOrRegular = 'error';
                console.log("CCI Error.");
                return;
            }
            $scope.chartData["CCI"] = data.JSONdata;
            $scope.chartData["CCI"]["dates"] = data.dates;
            $scope.hideCCIProgressBar = true;
            if($scope.chartActive == "CCI"){
                $scope.drawChart("CCI");
            }
            console.log("CCI has received.");
        })
        .error(function(data, status, headers, config){
            $scope.CCIErrorOrRegular = 'error';
            console.log("CCI Fail.");
        });
        $http({
            url : "http://127.0.0.1:3000/Indicator",
            method : "GET",
            params : {symbol : $scope.symbol, indicator : "BBANDS"}
        }).success(function(data, status, headers, config){
            if(data.JSONdata == "APIError"){
                $scope.BBANDSErrorOrRegular = 'error';
                console.log("BBANDS Error.");
                return;
            }
            $scope.chartData["BBANDS"] = data.JSONdata;
            $scope.chartData["BBANDS"]["dates"] = data.dates;
            $scope.hideBBANDSProgressBar = true;
            if($scope.chartActive == "BBANDS"){
                $scope.drawChart("BBANDS");
            }
            console.log("BBANDS has received.");
        })
        .error(function(data, status, headers, config){
            $scope.BBANDSErrorOrRegular = 'error';
            console.log("BBANDS Fail.");
        });
        $http({
            url : "http://127.0.0.1:3000/Indicator",
            method : "GET",
            params : {symbol : $scope.symbol, indicator : "MACD"}
        }).success(function(data, status, headers, config){
            if(data.JSONdata == "APIError"){
                $scope.MACDErrorOrRegular = 'error';
                console.log("MACD Error.");
                return;
            }
            $scope.chartData["MACD"] = data.JSONdata;
            $scope.chartData["MACD"]["dates"] = data.dates;
            $scope.hideMACDProgressBar = true;
            if($scope.chartActive == "MACD"){
                $scope.drawChart("MACD");
            }
            console.log("MACD has received.");
        })
        .error(function(data, status, headers, config){
            $scope.MACDErrorOrRegular = 'error';
            console.log("MACD Fail.");
        });

        // get 1000 days time series
        $http({
            url : "http://127.0.0.1:3000/History",
            method : "GET",
            params : {symbol : string}
        }).success(function(data, status, headers, config){
            //console.log(data);
            if(data.JSONdata == "APIError"){
                $scope.HistoryErrorOrRegular = 'error';
                console.log("TimeSeries Error.");
                return;
            }
            $scope.chartData['history'] = data.JSONdata;
            $scope.hideHistoryProgressBar = true;
            if($scope.hideHistory){
                $scope.drawHistory();
            }
        })
        .error(function(data, status, headers, config){
            $scope.HistoryErrorOrRegular = 'error';
            console.log("TimeSeries Fail");
        });

        // get news data
        $http({
            url : "http://127.0.0.1:3000/NewsFeed",
            method : "GET",
            params : {symbol : string}
        }).success(function(data, status, headers, config){
            $scope.newsData = data.JSONdata;
            $scope.hideNewsProgressBar = true;
            console.log("News has received.");
            //console.log($scope.newsData);
        })
        .error(function(data, status, headers, config){
            $scope.NewsErrorOrRegular = 'error';
            console.log("Error");
        });
    }
    // active clicked nav-pills bar
    $scope.showCurrent = function(){
        $scope.currentActive = true;
        $scope.historicalActive = false;
        $scope.newsActive = false;

        $scope.hideCurrent = false;
        $scope.hideHistory = true;
        $scope.hideNews = true;
    }
    $scope.showHistory = function(){
        $scope.currentActive = false;
        $scope.historicalActive = true;
        $scope.newsActive = false;

        $scope.hideCurrent = true;
        $scope.hideHistory = false;
        $scope.hideNews = true;
        $scope.drawHistory();
    }
    $scope.showNews = function(){
        $scope.currentActive = false;
        $scope.historicalActive = false;
        $scope.newsActive = true;

        $scope.hideCurrent = true;
        $scope.hideHistory = true;
        $scope.hideNews = false;
    }
    // active and show content of nav-bar
    $scope.drawPriceChart = function(){
        console.log("now draw price");
        if($scope.ngSwitchSelected == "item1"){
            return;
        }
        if($scope.chartData === undefined || $scope.chartData["price"] === undefined || $scope.detailStatisticData === undefined){
            console.log("price data hasn't received yet.");
            return;
        }
        var chartContent;
        var hasThisChart = false;
        //for(var i = 0; i < $scope.allChartStringData.length; i++){
        for(var i = 0; i < $localStorage.allChartStringData.length; i++){
            if($localStorage.allChartStringData[i].type == "price"){
                if($localStorage.allChartStringData[i].data.Series === undefined || $localStorage.allChartStringData[i].data.Series.length == 0){
                    chartContent = $localStorage.allChartStringData[i].data;
                    hasThisChart = true;
                }
                break;
            }
        }
        if(hasThisChart == false){
            var sym;
            if($scope.detailStatisticData === undefined){
                sym = $scope.symbol;
            } else {
                sym = $scope.detailStatisticData["Stock Ticker Symbol"];
            }
            var dates = $scope.chartData['price']["dates"];
            var Series = [];
            Series.push({
                name: sym,
                type: "area", 
                data: $scope.chartData["price"]["price"],
                yAxis: 0,
                color: "#ADD8E6",
                lineColor: '#000080'
            });
            Series.push({
                name: sym + " Volume",
                type: "column",
                data: $scope.chartData["price"]["volume"],
                yAxis: 1,
                color: "#FF0000"
            });
            chartContent = {
                chart: {
                    zoomType: 'x'
                },
                title: {
                    text: sym + " Stock Price and Volume",
                },
                subtitle: {
                    useHTML: true,
                    text: '<a style="color:blue;" target="_blank" href="https://www.alphavantage.co/">Source: Alpha Vantage</a>'
                },
                xAxis: {
                    categories: dates,
                    tickInterval: 5,
                    startOnTick: false
                },
                tooltip: {
                    formatter: function() {
                        if(this.series.name == sym){
                            return  this.x + "<br/><span style=\"color:" + this.color + "\">\u25CF</span> <b>" + this.series.name + '</b>: ' + Highcharts.numberFormat(this.y, 2);
                        // to disable the tooltip at a point return false 
                        }else {
                            return  this.x + "<br/><span style=\"color:" + this.color + "\">\u25CF</span> <b>" + this.series.name + '</b>: ' + Highcharts.numberFormat(this.y, 0, '.', ' ');
                        }
                    }
                },
                yAxis: [
                    {
                        title: {
                            text: "Stock Price"
                        }
                    },
                    {
                        title: {
                            text: "Volume"
                        },
                        opposite: true,
                        min: 0,
                        tickInterval: 50000000
                    }
                ],
                series: Series,
                plotOptions: {
                    series: {
                        lineWidth: 1,
                        marker: {
                            enabled: false
                        }
                    }
                }
            };
            // add to history chart string data
            $localStorage.allChartStringData.push({
                type: "price",
                data: chartContent
            });
        }
        Highcharts.chart('priceChart', chartContent);
    }
    $scope.drawChart = function(string) {
        console.log(string);
        if($scope.ngSwitchSelected == "item1"){
            return;
        }
        if(string == "SMA"){
            if($scope.SMAErrorOrRegular == "error"){
                return;
            }
        }
        if(string == "EMA"){
            if($scope.EMAErrorOrRegular == "error"){
                return;
            }
        }
        if(string == "STOCH"){
            if($scope.STOCHErrorOrRegular == "error"){
                return;
            }
        }
        if(string == "RSI"){
            if($scope.RSIErrorOrRegular == "error"){
                return;
            }
        }
        if(string == "ADX"){
            if($scope.ADXErrorOrRegular == "error"){
                return;
            }
        }
        if(string == "CCI"){
            if($scope.CCIErrorOrRegular == "error"){
                return;
            }
        }
        if(string == "BBANDS"){
            if($scope.BBANDSErrorOrRegular == "error"){
                return;
            }
        }
        if(string == "MACD"){
            if($scope.MACDErrorOrRegular == "error"){
                return;
            }
        }
        if($scope.chartData === undefined || $scope.chartData[string] === undefined){
            console.log(string + " data hasn't received yet.");
            return;
        }
        var chartName = string + "Chart";
        var chartTitle;
        if(string == "SMA"){
            chartTitle = "Simple Moving Average (SMA)";
        } else if(string == "EMA"){
            chartTitle = "Exponential Moving Average (EMA)";
        } else if(string == "STOCH"){
            chartTitle = "Stochastic (STOCH)";
        } else if(string == "RSI"){
            chartTitle = "Relative Strength Index (RSI)";
        } else if(string == "ADX"){
            chartTitle = "Average Directional Index (ADX)";
        } else if(string == "CCI"){
            chartTitle = "Commodity Channel Index (CCI)";
        } else if(string == "BBANDS"){
            chartTitle = "Bollinger Bands (BBANDS)";
        } else {
            chartTitle = "Moving Average Convergence/Divergence(MACD)";
        }
        var chartContent;
        var hasThisChart = false;
        for(var i = 0; i < $localStorage.allChartStringData.length; i++){
            if($localStorage.allChartStringData[i].type == string){
                if($localStorage.allChartStringData[i].data.Series === undefined || $localStorage.allChartStringData[i].data.Series == null || $localStorage.allChartStringData[i].data.Series == []){
                    chartContent = $localStorage.allChartStringData[i].data;
                    hasThisChart = true;
                }
                break;
            }
        }
        if(hasThisChart == false){
            var sym;
            if($scope.detailStatisticData === undefined){
                sym = $scope.symbol;
            } else {
                sym = $scope.detailStatisticData["Stock Ticker Symbol"];
            }
            var Series = [];
            var dates = [];
            var d = $scope.chartData[string];
            for(fieldName in d){
                if(fieldName == "dates"){
                    dates = d[fieldName];
                } else {
                    var n = sym + " " + fieldName;
                    Series.push({
                        name: n,
                        data: d[fieldName]
                    });
                }
            }
            chartContent = {
                chart: {
                    zoomType: 'x'
                },
                title: {
                    text: chartTitle
                },
                subtitle: {
                    useHTML: true,
                    text: '<a style="color:blue;cursor:pointer" target="_blank" href="https://www.alphavantage.co/">Source: Alpha Vantage</a>'
                },
                xAxis: {
                    categories: dates,
                    tickInterval: 5
                },
                yAxis: {
                    title: {
                        text: string
                    }
                },
                plotOptions: {
                    series: {
                        marker: {
                            enabled: true,
                            radius: 2
                        },
                        lineWidth: 1
                    }
                },
                tooltip: {
                    formatter: function() {
                        return  this.x + "<br/><span style=\"color:" + this.color + "\">\u25CF</span> <b>"+this.series.name + '</b>: ' + Highcharts.numberFormat(this.y, 2);
                    }
                },
                series: Series
            };
            //console.log(chartContent);
            // add to history chart string data
            $localStorage.allChartStringData.push({
                type: string,
                data: chartContent
            });
        }
        Highcharts.chart(chartName, chartContent);
    }
    $scope.drawHistory = function(){
        if($scope.ngSwitchSelected == "item1"){
            return;
        }
        if($scope.HistoryErrorOrRegular == "error"){
            return;
        }
        if($scope.chartData === undefined || $scope.chartData["history"] === undefined){
            return;
        }
        var sym;
        if($scope.detailStatisticData === undefined){
            sym = $scope.symbol;
        } else {
            sym = $scope.detailStatisticData["Stock Ticker Symbol"];
        }
        Highcharts.stockChart('historyChart', {
            rangeSelector: {
                selected: 1
            },
            title: {
                text: sym + ' Stock Value'
            },
            subtitle: {
                useHTML: true,
                text: '<a style="color:blue;cursor:pointer" target="_blank" href="https://www.alphavantage.co/">Source: Alpha Vantage</a>'
            },
            yAxis: {
                title: {
                    text: "Stock Value"
                }
            },
            series: [{
                name: sym,
                data: $scope.chartData["history"],
                type: "area"
            }],
            plotOptions: {
                series: {
                    marker: {
                        enabled: false,
                    },
                    lineWidth: 1
                }
            },
            tooltip: {
                formatter: function() {
                    var date = new Date(this.x);
                    var days = ['Sunday', 'Monday', 'Tuesday', 'Wednesday', 'Thursday', 'Friday', 'Saturday'];
                    var dayName = days[date.getDay()];
                    var months = ["Jan", "Feb", "Mar", "Apr", "May", "Jun", "Jul", "Aug", "Sept", "Oct", "Nov", "Dec"];
                    var monthName = months[date.getMonth()];
                    var day = date.getDate();
                    var hour = date.getHours();
                    var showDate = dayName + ", " + monthName + " " + day + ", " + hour + ":00";
                    return showDate + "<br/><span style=\"color:#ADD8E6\">\u25CF</span>"+ $scope.detailStatisticData["Stock Ticker Symbol"] + ':<b>' + this.y + "</b>";
                }
            }
        });
    }

    // deal with favorite list
    $scope.changeFavoriteStatus = function(){
        if($scope.hasBeenInFavoriteList){ // remove from favorite list
            for(var i = 0; i < $localStorage.favoriteListData.length; i++){
                if($localStorage.favoriteListData[i].symbol == $scope.symbol){
                    $localStorage.favoriteListData.splice(i, 1);
                    $localStorage.wholeFavoriteData.splice(i, 1);
                    $scope.hasBeenInFavoriteList = false;
                    return;
                }
            }
        }
        if($scope.detailStatisticData === undefined || $scope.chartData["price"] === undefined || $scope.detailErrorOrRegular == 'error' || $scope.priceErrorOrRegular == 'error'){
            console.log("haven't received stock detail data.");
            return;
        }
        for(var i = 0; i < $localStorage.favoriteListData.length; i++){
            //console.log($scope.favoriteListData[i].symbol, $scope.symbol);
            if($localStorage.favoriteListData[i].symbol == $scope.symbol){
                console.log("already in favorite list.");
                return;
            }
        }
        var ch = $scope.detailStatisticData["Change (Change Percent)"].split(" ");
        var cha = ch[0];
        var per = ch[1].slice(1, -1);
        var co = (cha > 0) ? "green" : "red";
        var tmp = {
            id : $scope.favoriteCount,
            symbol : $scope.symbol, 
            price : $scope.detailStatisticData["Last Price"].slice(1),
            change : cha,
            percent : per,
            volume : $scope.detailStatisticData["Volume"], 
            color: co
        };
        var tmp2 = {
            id : $scope.favoriteCount,
            symbol : $scope.symbol,
            chartData : $scope.chartData,
            statData : $scope.detailStatisticData,
            newsData : $scope.newsData,
        }
        $scope.favoriteCount++;
        $scope.hasBeenInFavoriteList = true;
        $localStorage.favoriteListData.push(tmp);
        $localStorage.wholeFavoriteData.push(tmp2);
        console.log($scope.favoriteListData);
    }
    $scope.deleteFromFavoriteList = function(id){
        for(var i = 0; i < $localStorage.favoriteListData.length; i++){
            if($localStorage.favoriteListData[i].id == id){
                $localStorage.favoriteListData.splice(i, 1);
                $localStorage.wholeFavoriteData.splice(i, 1);
                console.log($localStorage.favoriteListData);
                $scope.hasBeenInFavoriteList = false;
                return;
            }
        }
    }
    $scope.changeFavoriteListOrder = function(sortOption, orderOption) {
        //console.log($scope.favoriteListData[0][sortOption], sortOption);
        $localStorage.favoriteListData = $localStorage.favoriteListData.sort(function(a, b){
            var keyA = a[sortOption];
            var keyB = b[sortOption];
            console.log(keyA, keyB);
            if(keyA < keyB){
                if(orderOption == "Ascending"){
                    return -1;
                }else{
                    return 1;
                }
            }
            if(keyA > keyB){
                if(orderOption == "Ascending"){
                    return 1;
                }else{
                    return -1;
                }
            }
            return 0;
        });
    }
    $scope.redrawCharts = function(string){
        $scope.ngSwitchSelected = "item2";
        $timeout(function(){
            for(var i = 0; i < $localStorage.wholeFavoriteData.length; i++){
                if($localStorage.wholeFavoriteData[i].symbol == string){
                    $localStorage.detailStatisticData = $localStorage.wholeFavoriteData[i].statData;
                    $localStorage.chartData = $localStorage.wholeFavoriteData[i].chartData;
                    $localStorage.newsData = $localStorage.wholeFavoriteData[i].newsData;
                    if($scope.hideHistory == false){
                        $scope.drawHistory();
                    }
                    if($scope.chartActive == "price"){
                        $scope.drawPriceChart();
                    }
                    console.log($scope.chartActive);
                    var all_indicators = ["SMA", "EMA", "STOCH", "RSI", "ADX", "CCI", "BBANDS", "MACD"];
                    for(var j = 0; j < all_indicators.length; j++){
                        if($scope.chartActive == all_indicators[j]){
                            $scope.drawChart(all_indicators[i]);
                        }
                    }
                    console.log("redraw all charts!!");
                    break;
                }
            }
        }, 100);
    }

    function refreshOneDetail(string, nowId){
        $http({
            url : "http://127.0.0.1:3000/TimeSeries",
            method : "GET",
            params : {symbol : string}
        }).success(function(data, status, headers, config){
            //console.log(data);
            if(data.JSONdata == "APIError"){
                console.log(string + " Refresh Error.");
                return;
            }
            var co = (data.Statistic["Change"] > 0) ? "green" : "red";
            var stat = {
                id :nowId,
                symbol : string,
                price : data.Statistic["Last Price"],
                change : data.Statistic["Change"],
                percent : data.Statistic["Change Percent"],
                volume : data.Statistic["Volume"],
                color : co
            };
            for(var i = 0; i < $localStorage.favoriteListData.length; i++){
                if($localStorage.favoriteListData[i].id == nowId){
                    $localStorage.favoriteListData[i] = stat;
                    return;
                }
            }
        })
        .error(function(data, status, headers, config){
            console.log(string + " Refresh Fail");
        });
    }

    // for refresh
    $scope.refreshStockDetail = function(){
        var newDetails = [];
        console.log("yeah we refresh now.");
        for(var i = 0; i < $localStorage.favoriteListData.length; i++){
            refreshOneDetail($localStorage.favoriteListData[i].symbol, $localStorage.favoriteListData[i].id);
        }
    }
    $scope.changeAutoRefresh = function(){
        console.log("change auto." + $scope.toggleValue + $scope.autoRefreshStatus);
        if($scope.autoRefreshStatus == true){ // automatic refresh off
            $interval.cancel($scope.autorefreshPromise);
        } else {
            $interval.cancel($scope.autorefreshPromise);
            $scope.autorefreshPromise = $interval($scope.refreshStockDetail, 8000);
        }
        $scope.autoRefreshStatus = !$scope.autoRefreshStatus;
    }

    // for sharing to facebook
    $scope.fbShare = function(chartName){
        console.log("share on fb");
        //console.log($scope.allChartStringData);
        for(var i = 0; i < $localStorage.allChartStringData.length; i++){
            if($localStorage.allChartStringData[i].type == chartName){
                var optionsStr = JSON.stringify($localStorage.allChartStringData[i].data);
                $http({
                    url : "http://127.0.0.1:3000/ChartConvert",
                    method : "GET",
                    params : {chartString : optionsStr}
                }).success(function(data, status, headers, config){
                    var imgUrl = "http://export.highcharts.com/" + data.JSONdata;
                    console.log(imgUrl);
                    FB.ui(
                    {
                        method: 'feed',
                        name: 'Facebook Dialogs',
                        link: imgUrl,
                        picture: 'http://fbrell.com/f8.jpg',
                        caption: 'Reference Documentation',
                        description: 'Dialogs provide a simple, consistent interface for applications to interface with users.'
                    }, function(response){});
                })
                .error(function(data, status, headers, config){
                    console.log("ChartConvert Fail");
                });
                return;
            }
        }

    }
    // switch second div
    $scope.changeItem = function(){
        if($scope.ngSwitchSelected == "item1"){
            $scope.ngSwitchSelected = "item2";
        } else {
            $scope.ngSwitchSelected = "item1";
        }
    }
});