/* This function is used to  modify the hrefs of the trend graphs 
 * in the Questa trend graphs portlet
 */
function resolveImagesHref() {
	var myMap = jobNametoMapId();
	var imagesLazyMapRefs = document.getElementsByTagName("map");
	for (var i = 0; i < imagesLazyMapRefs.length; i++) {
		var mapName = imagesLazyMapRefs[i].getAttribute("name");
		var children = imagesLazyMapRefs[i].children;
		for (var j = 0; j < children.length; j++) {
			var origHref = children[j].getAttribute("href");
			var prependedHref = "job/" + myMap[mapName] + "/" + origHref;
			children[j].setAttribute("href", prependedHref);
		}
	}
}

function jobNametoMapId() {
	/* select all image tags having lazymap */
	var myMap = {};
	var images = document.getElementsByTagName("img");
	for (var i = 0; i < images.length; i++) {
		var lazyMap = images[i].getAttribute("lazymap");
		if (lazyMap != null) {
			var jobName = lazyMap.split("/")[1];
			var usingMap = images[i].getAttribute("usemap").substring(1); // remove the '#' character from the beginning
			myMap[usingMap] = jobName;
		}
	}
	return myMap;
}