var $JQuery = jQuery.noConflict();
$JQuery('#addDonorForm').submit(function(event)
{
	$JQuery("#loading").show();
	event.preventDefault();

	var url = "/openmrs/module/sharedhealthrecord/addDonor.form";
	var token = $JQuery("meta[name='_csrf']").attr("content");
	var header = $JQuery("meta[name='_csrf_header']").attr("content");

	// var name = document.getElementById('categoryName');
	var formData;
	formData = {
		'donorName' : $JQuery('#donorName').val()
	};
//	console.table(formData);
	$JQuery.ajax({
		contentType: "application/json",
		type: "POST",
		url: url,
		data: JSON.stringify(formData),
		dataType: 'json',

		timeout: 100000,
		beforeSend: function(xhr){
			xhr.setRequestHeader(header, token);
		},
		success : function(data){
			$JQuery('#loading').hide();
			if(data == "Success"){
				window.location.replace("/openmrs/module/sharedhealthrecord/donorList.form");
			}
			else {
				$JQuery('#donornameUniqueErrorMessage').html(data);
				
			}
		},
		error: function(data){
			$JQuery('#loading').hide();
		}
	});
});
