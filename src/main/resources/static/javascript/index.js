var myEl = document.getElementById('button');
var path = document.getElementById('path')

//method for when the method is clicked that returns the histogram
myEl.addEventListener('click', function() {

    var map = {"1" : path.value};

    $.ajax({
        type: 'POST',
        url:  "http://localhost:8080"+"/path/words",
        data: JSON.stringify(map),
        success: function(response) {

            wordReport.run(response);

        },
        contentType: "application/json",
        dataType: 'json'
    });

}, false);

//method that formats the data in the map so that it fits the d3 graph's format
formatWordsFromMap = function(map){

    var pairs = _.pairs(map["1"]);
    var arrayLength = pairs.length;
    var dataArray = [];
    for (var i = 0; i < arrayLength; i++) {
        var wordCount = {word: pairs[i][0], count: pairs[i][1]};
        dataArray.push(wordCount);
    }
    var dataSet = [{data: dataArray}];
    return dataSet;
};


var wordReport = {};

wordReport.run = function(wordMap){

    var numWords = Object.keys(wordMap[1]).length;

    var margins = {
            top: 12,
            left: 100,
            right: 24,
            bottom: 24
        },
        legendPanel = {
            width: 180
        },
        width = 1000 - margins.left - margins.right - legendPanel.width,
        height = (50*numWords) - margins.top - margins.bottom,

        dataset = formatWordsFromMap(wordMap),

        dataset = dataset.map(function (d) {
            return d.data.map(function (o, i) {
                return {
                    y: o.count,
                    x: o.word
                };
            });
        }),
        stack = d3.layout.stack();

    stack(dataset);

    var dataset = dataset.map(function (group) {
            return group.map(function (d) {
                // Invert the x and y values, and y0 becomes x0
                return {
                    x: d.y,
                    y: d.x,
                    x0: d.y0
                };
            });
        }),
        svg = d3.select('body')
            .append('svg')
            .attr('width', width + margins.left + margins.right + legendPanel.width)
            .attr('height', height + margins.top + margins.bottom)
            .append('g')
            .attr('transform', 'translate(' + margins.left + ',' + margins.top + ')'),
        xMax = d3.max(dataset, function (group) {
            return d3.max(group, function (d) {
                return parseInt(d.x);
            });
        }),
        xScale = d3.scale.linear()
            .domain([0, xMax])
            .range([0, width]),
        words = dataset[0].map(function (d) {
            return d.y;
        }),
        _ = console.log(words),
        yScale = d3.scale.ordinal()
            .domain(words)
            .rangeRoundBands([0, height], .1),
        xAxis = d3.svg.axis()
            .scale(xScale)
            .orient('bottom'),
        yAxis = d3.svg.axis()
            .scale(yScale)
            .orient('left'),
        colours = d3.scale.category10(),
        groups = svg.selectAll('g')
            .data(dataset)
            .enter()
            .append('g')
            .style('fill', function (d, i) {
                return colours(i);
            }),
        rects = groups.selectAll('rect')
            .data(function (d) {
                return d;
            })
            .enter()
            .append('rect')
            .attr('x', function (d) {
                return xScale(d.x0);
            })
            .attr('y', function (d, i) {
                return yScale(d.y);
            })
            .attr('height', function (d) {
                return yScale.rangeBand();
            })
            .attr('width', function (d) {
                return xScale(d.x);
            })
            .on('mouseover', function (d) {
                var xPos = parseFloat(d3.select(this).attr('x')) / 2 + width / 2;
                var yPos = parseFloat(d3.select(this).attr('y')) + yScale.rangeBand() / 2;

                d3.select('#tooltip')
                    .style('left', xPos + 'px')
                    .style('top', yPos + 'px')
                    .select('#value')
                    .text('word: ' + d.y + '  count: ' + d.x);

                d3.select('#tooltip').classed('hidden', false);
            })
            .on('mouseout', function () {
                d3.select('#tooltip').classed('hidden', true);
            })

    svg.append('g')
        .attr('class', 'axis')
        .attr('transform', 'translate(0,' + height + ')')
        .call(xAxis);

    svg.append('g')
        .attr('class', 'axis')
        .call(yAxis);

}






