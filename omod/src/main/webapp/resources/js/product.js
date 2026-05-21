var $JQuery = jQuery.noConflict();
$JQuery("#serviceForm").submit(function(event) {
	event.preventDefault();
	if($JQuery('input[name=name]').val().trim() == "" || $JQuery('input[name=code]').val().trim() == "") {
		$JQuery("#usernameUniqueErrorMessage").html("Product Name and Code are required field");
		$JQuery("#usernameUniqueErrorMessage").show();
		return;

	}
	$JQuery("#loading").show();
	$JQuery("#usernameUniqueSuccessMessage").hide();
	$JQuery("#usernameUniqueErrorMessage").hide();
	var url = "/openmrs/ws/rest/v1/service-management/save";
	var token = $JQuery("meta[name='_csrf']").attr("content");
	var header = $JQuery("meta[name='_csrf_header']").attr("content");

	var e = document.getElementById("category");
	var category = e.options[e.selectedIndex].value;

	var doseTypevalue = document.getElementById("doseType");
	var doseTypeValueSelected = doseTypevalue.options[doseTypevalue.selectedIndex].value;

	var clinicValue = $JQuery('input[name=tdhClinicManagement]').val();
	//
	var voided = null;
	var disableConfirmation = $JQuery('input[name=disableService]:checked').val();
	var enableConfirmation = $JQuery('input[name= ]:checked').val();
	if (disableConfirmation == "yes") {
		voided = true;
	}
	if (disableConfirmation == "no") {
		voided = false;
	}
	if (enableConfirmation == "yes") {
		voided = false;
	}

			var formData;			
				formData = {
			             'name': $JQuery('input[name=name]').val(),
			             'code': $JQuery('input[name=code]').val(),
			             'category': category,
			             'medicinId':  $JQuery('input[name=medicinId]').val(),
			             'tdhClinicManagement': clinicValue,
					     'genericDescription': $JQuery('input[name=genericDescription]').val(),
					     'dose': $JQuery('input[name=dose]').val(),
					     'projectName': $JQuery('input[name=projectName]').val(),
					     'packetSize': $JQuery('input[name=packetSize]').val(),
					     'unitPrice': $JQuery('input[name=unitPrice]').val(),
					     'doseType': doseTypeValueSelected,
			             'voided': voided,
			        };			
			
			console.log(formData);
			
			$JQuery.ajax({
				contentType : "application/json",
				type: "POST",
		        url: url,
		        data: JSON.stringify(formData), 
		        dataType : 'json',
		        
				timeout : 100000,
				beforeSend: function(xhr) {				    
					 xhr.setRequestHeader(header, token);
				},
				success : function(data) {
				   $JQuery("#loading").hide();
				   if(data == ""){	
						$JQuery("#usernameUniqueSuccessMessage").html("Product Successfully Saved");
						$JQuery("#usernameUniqueSuccessMessage").show();
					   window.location.replace("/openmrs/module/sharedhealthrecord/product-list.form?id="+clinicValue);
				   }
				   else {
						$JQuery("#usernameUniqueErrorMessage").html(data);
						$JQuery("#usernameUniqueErrorMessage").show();
				   }
				   
				},
				error : function(e) {
				   
				},
				done : function(e) {				    
				    console.log("DONE");				    
				}
			}); 
		});