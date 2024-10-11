var app = angular.module('stockDetail', []);

var stockName = document.getElementById("stockName").textContent;
var day = document.getElementById("day").textContent;
var ctx = document.getElementById('stockGraph');

// configure controller
app.controller('stockDetailController', function($interval, $scope, $http) {

	var d = new Date();
	$scope.selectedHour = d.getHours();
	$scope.selectedMinute = d.getMinutes();
	$scope.minuteOffSet = 0;

	let getData = function(url) {
		return $http({
			method: 'GET',
			url: url
		});
	};

	/*
	* Populate current time selection options
	*/
	let populateTimeSelect = function() {
		$scope.hourRange = [];
		for (var i = 0; i < 24; i++) {
			$scope.hourRange.push(i);
		}

		$scope.minuteRange = [];
		for (var i = 0; i < 60; i++) {
			$scope.minuteRange.push(i);
		}
	};

	let populateBuyOrderTypes = function() {
		// load types of Buy orders supported
		getData('/order/buyOrderTypes').then(function(data) {
			$scope.buyOrderTypes = data.data;

		}, function(error) {

		});
	}

	/*
	* Populate current time selection options
	*/
	let updateData = function() {
		if ($scope.stockChart) {
			$scope.minuteOffSet++;

			var totalMins = ($scope.selectedHour * 60) + $scope.selectedMinute + $scope.minuteOffSet;
			var offsetHour = Math.floor(totalMins / 60);
			var offsetMinute = totalMins - (offsetHour * 60);
			var chartLabelData = $scope.stockData;
			var chartLabelDataLastVal = chartLabelData[chartLabelData.length - 1].time;
			var lastDataHourMin = chartLabelDataLastVal.replace(":", "");
			var currentDataHourMin = offsetHour + '' + offsetMinute;

			// cancel periodic method invocation if past last data point
			if (Number(currentDataHourMin) >= Number(lastDataHourMin)) {
				$interval.cancel($scope.intervalUpdateData);
				$scope.intervalUpdateData = -1;
			} else {
				// if within time range get data and show
				var urlParams = 'stockName=' + stockName + '&day=' + day;
				urlParams += "&hour=" + $scope.selectedHour;
				urlParams += "&minute=" + $scope.selectedMinute;
				urlParams += "&minuteOffset=" + $scope.minuteOffSet;

				getData('/order/getPrice?' + urlParams).then(function(data) {
					delta = data.data;
					if (delta[0] != '') {
						drawGraph($scope, delta);
					}
				}, function(error) {

				});
			}
		}
	};

	// on page load get list of stocks
	getData('/order/getStockData?stockName=' + stockName + '&day=' + day).then(function(data) {
		$scope.stockData = data.data;
		drawGraph($scope, null);
	}, function(error) {

	});

	let showBuyTypeOptions = function() {
		$scope.buyOptionSelected = true;
		$scope.selectedBuyType.values = [];
		for (var i = 0; i < $scope.selectedBuyType.Fields.length; i++) {
			$scope.selectedBuyType.values.push(0.0);
		}
	};
	$scope.showBuyTypeOptions = showBuyTypeOptions;

	let submitBuyOrder = function() {
		$http({
			method: 'POST',
			url: '/order/submitBuyOrder',
			data: $scope.selectedBuyType
		}).then(function(res) {
			if(res.data && res.data.length > 0)
				$scope.submitBuyMsg = res.data[0];
			else
				$scope.submitBuyMsg = "success";
		}, function(err) {
			/*if(err.data && err.data.error)
				$scope.submitBuyError = err.data.error.substring(0, 15);
			else
				$scope.submitBuyError = err.substring(0, 15);*/
			$scope.submitBuyMsg = "Error ! Order Not Submitted";
		});
	};
	$scope.submitBuyOrder = submitBuyOrder;

	// redraw graph if requested for user events
	let redrawGraphForTimeChange = function() {
		$scope.minuteOffSet = 0;
		drawGraph($scope, null);
		if ($scope.intervalUpdateData == -1)
			$scope.intervalUpdateData = $interval(updateData, 60000);
	};

	populateTimeSelect();
	$scope.redrawGraphForTimeChange = redrawGraphForTimeChange;
	$scope.intervalUpdateData = $interval(updateData, 60000);
	populateBuyOrderTypes();
});

/*
* This function is used to display stock data 
* till the user's local time on a chart.
*/
function drawGraph($scope, appendData) {
	var xaxis = [];
	var yaxis = [];
	var minVal = 0
	var maxVal = 0;

	// append data if present to existing chart
	if (appendData) {
		xaxis = $scope.stockChart.data.labels;
		yaxis = $scope.stockChart.data.datasets[0].data;
		minVal = $scope.stockChart.options.scales.y.min;
		maxVal = $scope.stockChart.options.scales.y.max;

		var newValX = appendData[0];
		var newValY = parseFloat(appendData[1]);

		xaxis.push(newValX);
		yaxis.push(newValY);

		if (newValY < minVal) {
			minVal = newValY;
			minVal = 0.999 * minVal;
		} else if (newValY > maxVal) {
			maxVal = newValY;
			maxVal = 1.001 * maxVal;
		}
	} else if ($scope.stockData) {
		// if no data to append, update existing graph to selected current time
		var currentTime = 0;
		if ($scope.selectedMinute < 10)
			currentTime = Number($scope.selectedHour + '0' + $scope.selectedMinute);
		else
			currentTime = Number($scope.selectedHour + '' + $scope.selectedMinute);

		var dataArr = $scope.stockData;
		minVal = dataArr[0].open;

		for (var i = 0; i < dataArr.length; i++) {
			var data = dataArr[i];
			var time = data.time;
			var value = data.open;

			if (Number(time.replace(":", "")) < currentTime) {
				xaxis.push(time);
				yaxis.push(value);

				if (value > maxVal)
					maxVal = value;
				if (value < minVal)
					minVal = value;
			}
		}

		maxVal = 1.001 * maxVal;
		minVal = 0.999 * minVal;
	}

	// if chart already present, update it, otherwise create new chart
	if ($scope.stockChart) {
		$scope.stockChart.data.labels = xaxis;
		$scope.stockChart.data.datasets[0].data = yaxis;
		$scope.stockChart.options = {
			scales: {
				y: {
					beginAtZero: false,
					min: minVal,
					max: maxVal,
					grid: {
						display: false
					}
				},
				x: {
					grid: {
						display: false
					}
				}
			}
		};
		$scope.stockChart.update();
	} else {
		$scope.stockChart = new Chart(ctx, {
			type: 'line',
			data: {
				labels: xaxis,
				datasets: [{
					label: 'Open Value',
					data: yaxis,
					backgroundColor: 'rgba(75, 192, 192, 0.2)',
					borderColor: 'rgba(75, 192, 192, 1)',
					borderWidth: 1,
					showLine: true,
					spanGaps: true
				}]
			},
			options: {
				scales: {
					y: {
						beginAtZero: false,
						min: minVal,
						max: maxVal,
						grid: {
							display: false
						}
					},
					x: {
						grid: {
							display: false
						}
					}
				}
			}
		});
	}
};
