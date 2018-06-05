var express = require('express');
const app = express();
const REQUEST = require('request');
const parseString = require('xml2js').parseString;
const API_KEY = "&&&&";
const Length = 140;
const date = new Date();

app.all('*', function(req, res, next) {
    res.header('Access-Control-Allow-Origin', '*');
    res.header('Access-Control-Allow-Methods', 'PUT, GET, POST, DELETE, OPTIONS');
    res.header('Access-Control-Allow-Headers', 'Content-Type');
    next();
});

app.get('/', function(request, response) {
    response.sendFile(__dirname + "/" + "StockSearch.html");
});

app.get('/StockSearch.css', function(request, response) {
    response.sendFile(__dirname + "/" + "StockSearch.css");
});

app.get('/StockSearch.js', function(request, response) {
    response.sendFile(__dirname + "/" + "StockSearch.js");
});

// for bootstrap-toggle
app.get('/angular-bootstrap-toggle.min.css', function(request, response) {
    response.sendFile(__dirname + "/" + "angular-bootstrap-toggle.min.css");
});
app.get('/angular-bootstrap-toggle.min.js', function(request, response) {
    response.sendFile(__dirname + "/" + "angular-bootstrap-toggle.min.js");
});

app.get('/AutoComplete', function(request, response){
    var Symbol = request.query.symbol;
    var URL = "&&&&" + Symbol;

    REQUEST({
        url : URL,
        json : true,
        headers: {
            'Accept': 'application/json',
            'Accept-Charset': 'utf-8',
        }
    }, function(err, res, body) {
        if(!err && res.statusCode == 200) {
            response.json({JSONdata : body});
            console.log("[Success]: AutoComplete finished, at " + date.toLocaleString()+" , URL: " + URL);
        }
    }); // end REQUEST
});

app.get('/History', function(request, response){
    var Symbol = request.query.symbol;
    var URL = "&&&" + Symbol + "&apikey=" + API_KEY;

    REQUEST({
        url : URL,
        json : true,
        headers: {
            'Accept': 'application/json',
            'Accept-Charset': 'utf-8',
        }
    }, function(err, res, body) {
        if(!err && res.statusCode == 200) {
            console.log(URL);
            // get JSON fields
            var all_fields = [];
            for (field in body){
                all_fields.push(field);
            }
            //console.log(all_fields);
            if(body == {} || all_fields[0] == "Error Message"){
                response.json({JSONdata : "APIError"});
                console.log("[Error]: History API error 1, at "+ date.toLocaleString()+" , URL: " + URL);
                return;
            }
            var cnt = 0;
            var d = [];
            if(body[all_fields[1]] === undefined){
                response.json({JSONdata : "APIError"});
                console.log("[Error]: History API error 2, at "+ date.toLocaleString()+" , URL: " + URL);
                return;
            }
            var dates = [];
            for(day in body[all_fields[1]]) { // Time Series (Daily)
                if(cnt > 1000) {
                    break;
                }
                var timestamp = new Date(day);
                timestamp = timestamp.getTime();
                d.push([timestamp, parseFloat(body[all_fields[1]][day]["4. close"])]);
                var little_day = (day.split(' ')[0]).split('-');
                dates.push(little_day[1] + "/" + little_day[2]);
                cnt += 1;
            }
            d = d.reverse();
            dates = dates.reverse();
            response.json({JSONdata : d, dates: dates});
            console.log("[Success]: History finished, at " + date.toLocaleString()+" , URL: " + URL);
        }
    }); // end REQUEST
});

app.get('/TimeSeries', function(request, response){
    var Symbol = request.query.symbol;
    var URL = "&&&" + Symbol + "&apikey=" + API_KEY;

    REQUEST({
        url : URL,
        json : true,
        headers: {
            'Accept': 'application/json',
            'Accept-Charset': 'utf-8',
        }
    }, function(err, res, body) {
        if(!err && res.statusCode == 200) {
            // get JSON fields
            var all_fields = [];
            for (field in body){
                all_fields.push(field);
            }
            //console.log(all_fields);
            if(body == {} || all_fields[0] == "Error Message"){
                response.json({JSONdata : "APIError"});
                console.log("[Error]: Time Series API error 1, at "+ date.toLocaleString()+" , URL: " + URL);
                return;
            }
            var cnt = 0;
            var volume = [];
            var price = [];
            var lastTwoDay =[];
            var dates = [];
            if(body[all_fields[1]] === undefined){
                response.json({JSONdata : "APIError"});
                console.log("[Error]: Time Series API error 2, at "+ date.toLocaleString()+" , URL: " + URL);
                return;
            }
            for(day in body[all_fields[1]]) { // Time Series (Daily)
                if(cnt > Length) {
                    break;
                }
                volume.push(parseInt(body[all_fields[1]][day]["5. volume"]));
                price.push(parseFloat(body[all_fields[1]][day]["4. close"]));
                var little_day = (day.split(' ')[0]).split('-');
                dates.push(little_day[1] + "/" + little_day[2]);
                if(cnt < 2){
                    lastTwoDay.push(body[all_fields[1]][day]);
                }
                cnt += 1;
            }
            //console.log(lastTwoDay);
            if(body[all_fields[0]] === undefined){
                response.json({JSONdata : "APIError"});
                console.log("[Error]: Time Series API error 3, at "+ date.toLocaleString()+" , URL: " + URL);
                return;
            }
            var timestamp = body[all_fields[0]]["3. Last Refreshed"];
            var close = lastTwoDay[0]["4. close"];
            var pirorClose = lastTwoDay[1]["4. close"];
            var change = (close - pirorClose).toFixed(2);
            var changePercent = (change * 100 / pirorClose).toFixed(2);
            var stat = {
                "Stock Ticker Symbol" : Symbol,
                "Last Price" : "$" + parseFloat(close).toFixed(2),
                "Change" : parseFloat(change).toFixed(2),
                "Change Percent" : parseFloat(changePercent).toFixed(2),
                "Timestamp" : timestamp,
                "Open" : parseFloat(lastTwoDay[0]["1. open"]).toFixed(2),
                "Close" : parseFloat(close).toFixed(2),
                "Day's Range" : parseFloat(lastTwoDay[0]["3. low"]).toFixed(2) + " - " + parseFloat(lastTwoDay[0]["2. high"]).toFixed(2),
                "Volume" : parseInt(lastTwoDay[0]["5. volume"]).toLocaleString()
            };
            //console.log(data);
            volume = volume.reverse();
            price = price.reverse();
            dates = dates.reverse();
            response.json({
                JSONdata : {
                    "volume" : volume,
                    "price" : price,
                    "dates" : dates
                }, 
                Statistic : stat
            });
            console.log("[Success]: Time Series finished, at " + date.toLocaleString()+" , URL: " + URL);
        }
    }); // end REQUEST
});

app.get('/Indicator', function(request, response) { // SMA EMA RSI
    var Symbol = request.query.symbol;
    var Indicator = request.query.indicator;
    var URL = "&&&" + Indicator +"&symbol=" + Symbol + "&&&" + API_KEY;
    if(Indicator == "STOCH") {
        URL = "&&&" + Symbol + "&&&" + API_KEY;
    }
    if(Indicator == "BBANDS") {
        URL = "&&&" + Symbol + "&&&" + API_KEY;
    }

    REQUEST({
        url : URL,
        json : true,
        headers: {
            'Accept': 'application/json',
            'Accept-Charset': 'utf-8',
        }
    }, function(err, res, body) {
        if(!err && response.statusCode == 200) {
            // get JSON fields
            var all_fields = [];
            for (field in body){
                all_fields.push(field);
            }
            if(body == {} || all_fields[0] == "Error Message"){
                response.json({JSONdata : "APIError"});
                console.log("[Error]: " + Indicator + " API error 1, at "+ date.toLocaleString()+" , URL: " + URL);
                return;
            }
            var cnt = 0;
            var data = [];
            var dates = [];
            if(body[all_fields[1]] === undefined){
                console.log(body);
                response.json({JSONdata : "APIError"});
                console.log("[Error]: " + Indicator + " API error 2, at "+ date.toLocaleString()+" , URL: " + URL);
                return;
            }
            for(day in body[all_fields[1]]) { // Technical Analysis
                if(cnt > Length) {
                    break;
                }
                data.push(body[all_fields[1]][day]);
                var little_day = (day.split(' ')[0]).split('-');
                dates.push(little_day[1] + "/" + little_day[2]);
                cnt += 1;
            }

            var sub_fields = [];
            for(field in data[0]){
                sub_fields.push(field);
            }
            var real_data = {};
            for(i = 0; i < sub_fields.length; i++){
                real_data[sub_fields[i]] = [];
            }
            for(i = 0; i < data.length; i++){
                for(j = 0; j < sub_fields.length; j++){
                    real_data[sub_fields[j]].push(parseFloat(data[i][sub_fields[j]]));
                }
            }
            for(i = 0; i < sub_fields.length; i++){
                real_data[sub_fields[i]] = real_data[sub_fields[i]].reverse();
            }
            dates = dates.reverse();
            //console.log(real_data);
            response.json({JSONdata : real_data,
                "dates" : dates});
            console.log("[Success]: " + Indicator + " finished, at " + date.toLocaleString()+" , URL: " + URL);
        }
    });
});

app.get('/NewsFeed', function(request, response){
    var Symbol = request.query.symbol;
    var URL = "&&&" + Symbol + ".xml";
    //console.log(URL);
    REQUEST({
        url : URL,
        headers: {
            'Accept': 'text/xml',
            'Accept-Charset': 'utf-8',
        }
    }, function(err, res, body) {
        if(!err && res.statusCode == 200) {
            console.log(URL);
            // get JSON fields
            //console.log(body);
            parseString(body, function(err, result) {
                var items = result["rss"]["channel"][0]["item"];
                var news = [];
                var cnt = 0;
                for(var i=0; i < items.length && cnt < 5;i++){
                    if((items[i]["link"][0]).indexOf("article") !== -1){
                        var time = (items[i]["pubDate"][0]).split(" ");
                        time.pop();
                        time.push("EDT");
                        var tmp = {
                            "title" : items[i]["title"][0],
                            "link" : items[i]["link"][0],
                            "pubDate" : time.join(" "),
                            "author" : items[i]["sa:author_name"][0]
                        };
                        news.push(tmp);
                        cnt++;
                    }
                }
                response.json({JSONdata : news});
                console.log("[Success]: NewsFeed finished, at " + date.toLocaleString()+" , URL: " + URL);
            });
        }
        if(err){
            console.log(err);
        }
    }); // end REQUEST
});

app.get('/ChartConvert', function(request, response){
    var chartString = request.query.chartString;
    var myForm = {
        "async" : true,
        "type" : "jpeg",
        "width" : 400,
        "options" : chartString
    }
    var options = {
        url: "http://export.highcharts.com/",
        method: 'POST',
        form: myForm
    }
    REQUEST(options, function (err, res, body) {
        if (!err && response.statusCode == 200) {
            // Print out the response body
            //console.log(body);
            response.json({JSONdata : body});
            console.log("[Success]: ChartConvert finished, at " + date.toLocaleString()+" , Image URL: " + body);
        }
    })
});

app.use(function(req, res, next) {
  var err = new Error('Not Found');
  err.status = 404;
  next(err);
});

var server = app.listen(3000, function() {
    var host = server.address().address;
    var port = server.address().port;
    console.log("instance, with address http://%s:%s", host, port); // on which ip????
});