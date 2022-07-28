
function loadBitstream(src){

	let elem = document.getElementById("bitstreamWrapper");
	let bitstreamIframe = document.getElementById("bitstreamIframe");
	let alertDiv = document.getElementById("alertDiv");

	if (src.includes("isAllowed=y")){

		if( bitstreamIframe == null){
			bitstreamIframe = document.createElement("iframe");
			bitstreamIframe.setAttribute("id", "bitstreamIframe");
			bitstreamIframe.setAttribute("style", "border: none; height:650px; width:100%;");
		}

		bitstreamIframe.setAttribute("src", src);
		elem.appendChild( bitstreamIframe );

		if ( alertDiv !== null ){
			elem.removeChild(alertDiv);
		}

	}else{ 

		if( alertDiv == null){ 
			alertDiv = document.createElement("div");
			alertDiv.setAttribute("class", "alert alert-danger");
			alertDiv.setAttribute("id", "alertDiv");

			elem.appendChild(alertDiv);
			alertDiv.innerText = "Authentication is required to view documents!";
		}

		if (bitstreamIframe !== null){
			elem.removeChild(bitstreamIframe);
		}

	}

}

window.addEventListener("load", function () {

	/***
	 * checks for the first bitstream file that is viewable by the user
	 * @type {HTMLElement}
	 */

	let bitstreamWrapper = document.getElementById("bitstreamWrapper");

	if ( bitstreamWrapper != null ){

		let idVal = bitstreamWrapper.firstElementChild.getAttribute("id");
		bitstreamWrapper.innerHTML = "";
		loadBitstream( idVal );
	}

});