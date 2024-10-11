var app = angular.module('indexApp', ['ngRoute']);

// configure routes
/*app.config([
	'$routeProvider', '$locationProvider',
	function($routeProvider, $locationProvider) {

		$locationProvider.html5Mode(false).hashPrefix('!');

		$routeProvider.when('/:tabName', {
			templateUrl: function($routeParams) {
				let rTabName = $routeParams.tabName;
				return 'tabs/' + rTabName + '.html';
			},
			controller: 'control'
		});
	}
]);*/

// configure controller
app.controller('indexController', function($scope, $http) {
	
	// global object to hold selected stock and day
	$scope.selectStockDtls = {day : '', name : ''};
	
	let getData = function(url) {
		return $http({
			'method' : 'GET',
			'url' : url
		});
	};
	
	// on page load get list of stocks
	getData('/order/getListedStocks').then(function(data) {
		$scope.listedStocks = data.data;
	}, function(error) {
		
	});
	
	// get days listed for each stock
	$scope.showListedDaysForStock = function(stock) {
		$scope.selectedStock = stock;
		getData('/order/getListedDaysForStock?' + 'stockName=' + stock).then(function(data) {
			$scope.stockDaysList = data.data;
		}, function(error) {
					
		});
	};
	
	$scope.showStockPage = function(stock) {
		day = $scope.selectStockDtls.day;
	};
});
