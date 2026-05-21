var $JQuery = jQuery.noConflict();
function showError(message) {
	var toast = document.getElementById("error-toast");
	var toastMessage = document.getElementById("toast-message");

	toastMessage.innerHTML = message;
	toast.style.display = "block";

	// Hide after 3 seconds
	setTimeout(function () {
		toast.style.display = "none";
	}, 3000);
}

$JQuery("#clinicInfo").submit(function (event) {


    event.preventDefault();

    var url = "/openmrs/ws/rest/v1/facility/save";
    var token = $JQuery("meta[name='_csrf']").attr("content");
    var header = $JQuery("meta[name='_csrf_header']").attr("content");

    /* var e = document.getElementById("category");
    var category = e.options[e.selectedIndex].value; */

    var division = document.getElementById("divisionId");
    var divisionId = division.options[division.selectedIndex].value;

    var district = document.getElementById("districtId");
    var districtId = district.options[district.selectedIndex].value;

    var upazila = document.getElementById("upazilaId");
    var upazilaId = upazila.options[upazila.selectedIndex].value;

    var category = $JQuery('input[name=category]').val();

    var clinicName = $JQuery('input[name=name]').val();
    var clinicId = $JQuery('input[name=clinicId]').val();
    var category = $JQuery('input[name=category]').val();
    var address = $JQuery('input[name=address]').val();
    if(clinicName == ""){
        showError("Clinic Name is required.");
		return;
    }
    if(category == ""){
        showError("Category is required.");
        return;
    }
    if(address == ""){
        showError("Address is required.");
        return;
    }

    if(divisionId === ""){
        showError("Division is required.");
		return;
    }
    if(districtId === ""){
        showError("District is required.");
        return;
    }
    if(upazilaId === "") {
        showError("Upazila is required.");
        return;
    }

    $JQuery("#loading").show();

    var formData;
    formData = {
        'name': $JQuery('input[name=name]').val(),
        'clinicId': $JQuery('input[name=clinicId]').val(),
        'category': $JQuery('input[name=category]').val(),
        'address': $JQuery('input[name=address]').val(),
        'cid': $JQuery('input[name=cid]').val(),
        'description': $JQuery('input[name=description]').val(),
        'division': divisionId,
        'district': districtId,
        'upazila': upazilaId,
        'category': category
    };

    console.log(formData);

    $JQuery.ajax({
        contentType: "application/json",
        type: "POST",
        url: url,
        data: JSON.stringify(formData),
        dataType: 'json',

        timeout: 100000,
        beforeSend: function (xhr) {
            xhr.setRequestHeader(header, token);
        },
        success: function (data) {
            $JQuery("#usernameUniqueErrorMessage").html(data);
            $JQuery("#loading").hide();
            if (data.message == "" || data.status == "OK") {
                window.location.replace("/openmrs/module/facility/ClinicList.form");

            }

        },
        error: function (e) {
            console.log("ERROR: ", e);

        },
        done: function (e) {
            console.log("DONE");
        }
    });
});

$JQuery("#editClinicInfo").submit(function (event) {


	event.preventDefault();

	var isValid = true;
	var errorMessage = "";

	var url = "/openmrs/ws/rest/v1/facility/save";
	var token = $JQuery("meta[name='_csrf']").attr("content");
	var header = $JQuery("meta[name='_csrf_header']").attr("content");

	var division = document.getElementById("divisionId");
	var divisionId = division.options[division.selectedIndex].value;

	var district = document.getElementById("districtId");
	var districtId = district.options[district.selectedIndex].value;

	var upazila = document.getElementById("upazilaId");
	var upazilaId = upazila.options[upazila.selectedIndex].value;

	var category = $JQuery('input[name=category]').val();

	var clinicName = $JQuery('input[name=name]').val();
	var clinicId = $JQuery('input[name=clinicId]').val();
	var category = $JQuery('input[name=category]').val();
	var address = $JQuery('input[name=address]').val();
	var cid = $JQuery('input[name=cid]').val();



	if(clinicName == ""){

        showError("Clinic Name is required.");
		return;
	}
	if(clinicId == ""){

        showError("Clinic ID is required.");
		return;
	}
	if(address == ""){

        showError("Address is required.");
		return;
	}
	if(divisionId == ""){

        showError("Division is required.");
        return;

	}
	if(districtId == ""){

        showError("District is required.");
        return;

	}
	if(upazilaId == ""){
        showError("Upazila is required.");
		return;
	}


	$JQuery("#loading").show();

	var formData;
	formData = {
		'name': $JQuery('input[name=name]').val(),
		'clinicId': $JQuery('input[name=clinicId]').val(),
		'category': $JQuery('input[name=category]').val(),
		'address': $JQuery('input[name=address]').val(),
		'cid': $JQuery('input[name=cid]').val(),
		'description': $JQuery('input[name=description]').val(),
		'division': divisionId,
		'district': districtId,
		'upazila': upazilaId,
		'category': category,
		'cid': cid
	};

	console.log(formData);

	$JQuery.ajax({
		contentType: "application/json",
		type: "POST",
		url: url,
		data: JSON.stringify(formData),
		dataType: 'json',

		timeout: 100000,
		beforeSend: function (xhr) {
			xhr.setRequestHeader(header, token);
		},
		success: function (data) {
			$JQuery("#usernameUniqueErrorMessage").html(data);
			$JQuery("#loading").hide();
			if (data.message == "" || data.status == "OK") {
				window.location.replace("/openmrs/module/facility/ClinicList.form");

			}

		},
		error: function (e) {
			console.log("ERROR: ", e);

		},
		done: function (e) {
			console.log("DONE");
		}
	});
});


$JQuery("#divisionId").change(function (event) {
    var e = document.getElementById("divisionId");
    var divisionId = e.options[e.selectedIndex].value;
    var url = "/openmrs/module/facility/locations.form?locationId=" + divisionId;

    if (divisionId == "") {
        $JQuery("#districtId").html("");
        $JQuery("#upazilaId").html("");

    } else {
        event.preventDefault();
        $JQuery.ajax({
            type: "GET",
            contentType: "application/json",
            url: url,
            dataType: 'html',
            beforeSend: function () {

            },
            success: function (data) {
                $JQuery("#districtId").html(data);
            },
            error: function (e) {
                console.log("ERROR: ", e);
                display(e);
            },
            done: function (e) {
                console.log("DONE");
                //enableSearchButton(true);
            }
        });
    }

});

$JQuery("#districtId").change(function (event) {
    var e = document.getElementById("districtId");
    var districtId = e.options[e.selectedIndex].value;
    var url = "/openmrs/module/facility/locations.form?locationId=" + districtId;
    if (districtId == "") {
        $JQuery("#upazilaId").html("");
    } else {
        event.preventDefault();
        $JQuery.ajax({
            type: "GET",
            contentType: "application/json",
            url: url,
            dataType: 'html',
            beforeSend: function () {

            },
            success: function (data) {
                $JQuery("#upazilaId").html(data);
            },
            error: function (e) {
                console.log("ERROR: ", e);
                display(e);
            },
            done: function (e) {
                console.log("DONE");
                //enableSearchButton(true);
            }
        });
    }
});