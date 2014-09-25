var POINTS_EXIST_IN_DAV_STYLE={color: '#FF0000', fillColor: '#FF8080', fillOpacity: 0.8};
var POINTS_EXIST_OTHER_IN_DAV_STYLE={color: '#FFA500', fillColor: '#FFA500', fillOpacity: 0.8};
var POINTS_NON_EXIST_IN_DAV_STYLE={color: '#000000', fillColor: '#000000', fillOpacity: 0.8};
var definedCircles = [];
var undefinedCircles = [];

var scope;

function Dav($scope) {
	$scope.rajony={};
	scope=$scope;
	for(p in data.dav) {
		var c = 0;
		for(vk in data.dav[p]) {
			if (data.dav[p][vk].osmID == undefined) {
				c++;
			}
		}
		$scope.rajony[p]=p+" ("+c+")";
	}
	

	$scope.$watch('sielsaviet', function(s) {
		var vs=[]; // вёскі сельсавету+раёну з даведніка
		var davOsmIDs={}; // вёскі што паказваем як знойдзеныя
		var davOsmIDs2={}; // вёскі што паказваем як знойдзеныя але ў іншым сельсавеце
		for(vi in data.dav[$scope.rajon]) {
			var v=data.dav[$scope.rajon][vi];
			if (v.ss == s) {
				v.varyjanty='';
				if (v.varyjantBe) {
					v.varyjanty+=' ('+v.varyjantBe+')';
				}
				if (v.varyjantRu) {
					v.varyjanty+=' ('+v.varyjantRu+')';
				}
				vs.push(v);
				davOsmIDs[v.osmID]=v.ss;
			} else {
				davOsmIDs2[v.osmID]=v.ss;
			}
		}
		$scope.svioski = vs;

		var rOsm=data.padziel[$scope.rajon];
		removeDefinedCircles();
		angular.forEach(data.map[rOsm], function (v) { // вёскі мапы па раёну
			if (davOsmIDs[v.osmID]!==undefined) {
				var circle = L.circle([v.lat,v.lon], 500, POINTS_EXIST_IN_DAV_STYLE).addTo(map);
				circle.bindPopup(id2str(v.osmID)+"<br/>"+v.name+"/"+v.nameBe+'<br/>с/с:'+davOsmIDs[v.osmID]);
				definedCircles.push(circle);
			}
			if (davOsmIDs2[v.osmID]!==undefined) {
				var circle = L.circle([v.lat,v.lon], 500, POINTS_EXIST_OTHER_IN_DAV_STYLE).addTo(map);
				circle.bindPopup(id2str(v.osmID)+"<br/>"+v.name+"/"+v.nameBe+'<br/>с/с:'+davOsmIDs2[v.osmID]);
				definedCircles.push(circle);
			}
		});
	});
	$scope.$watch('rajon', function(r) {
		$scope.rvioski = data.dav[r];
		
		var davOsmIDs={};
		angular.forEach(data.dav[r], function (v) {
			if (v.osmID !== undefined) {
				davOsmIDs[v.osmID]=1;
			}
		});

		removeUndefinedCircles();
		var rOsm=data.padziel[r];
		angular.forEach(data.map[rOsm], function (v) {
			if (davOsmIDs[v.osmID]==undefined) {
				var circle = L.circle([v.lat,v.lon], 500, POINTS_NON_EXIST_IN_DAV_STYLE).addTo(map);
				circle.bindPopup(id2str(v.osmID)+"<br/>"+v.name+"/"+v.nameBe);
				undefinedCircles.push(circle);
			}
		});
	});
	
	$scope.sielsaviety = function(vioski) {
		var r={};
		for(vk in vioski) {
			r[vioski[vk].ss]=0;
		}
		for(vk in vioski) {
			if (vioski[vk].osmID == undefined) {
				r[vioski[vk].ss]++;
			}
		}
		for(ri in r) {
			if (r[ri]>0) {
				r[ri] = ri+"("+r[ri]+")";
			} else {
				r[ri] = ri;
			}
		}
		return r;
	};
}

function cempty(vioski) {
	c = 0;
	for(vk in vioski) {
		if (vioski[vk].osmID == undefined) {
			c++;
		}
	}
	return c;
}

function removeDefinedCircles() {
	angular.forEach(definedCircles, function (c) {
		map.removeLayer(c);
	});
	definedCircles = [];
}

function removeUndefinedCircles() {
	angular.forEach(undefinedCircles, function (c) {
		map.removeLayer(c);
	});
	undefinedCircles = [];
}

function id2str(id) {
	return id+" <a href='http://www.openstreetmap.org/node/"+id+"/history' target='_blank'><img src='http://wiki.openstreetmap.org/w/images/b/b5/Mf_node.png'/></a>";
}

