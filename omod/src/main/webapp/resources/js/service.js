var $jq = jQuery.noConflict();
$jq(window).load(function() {
	$jq("#loader").hide();
	$jq("#tabs").show(); 
	$jq("#monthlyDatePicker").hide();
});
$jq( function() {
	$jq("#startDate").datepicker({ dateFormat: 'yy-mm-dd', maxDate: new Date });
	$jq("#endDate").datepicker({ dateFormat: 'yy-mm-dd', minDate: new Date});
  } );
  
$jq("#startDate").on("change",function(){
	/* console.log("change"); */
	$jq("#endDate").datepicker(
			'option',
			{ minDate: new Date($jq("#startDate").val()),
			  maxDate: new Date()
			});
});

//$jq(document).ready( function () {
//	$jq('#loading_prov').show();
//	$jq('#reportTitle').html("Stock Report");
//	var title = $jq('#reportName').find('option:selected').text();
//	var tbaleData = window["SP_getmonthlystockreport"]();
//	$jq("#thead_id").html(tbaleData);
//	var clinicCode = $jq("#clinic").val();
//	$jq('#table_id').DataTable({
//        bFilter: false,
//        serverSide: false,
//        processing: true,
//	    "searching": true,
//        bInfo: false,
//        dom: 'Bfrtip',
//        buttons: [
//		             {
//		                 extend: 'excelHtml5',
//		                 title: title,
//		                 text: 'Export as .xlxs'
//		             }
//		             /*,
//		             {
//		         		extend: 'pdfHtml5',
//		         		//title: title,
//		         		text: 'Export as .pdf',
//		         		orientation: 'landscape',
//		         		pageSize: 'LEGAL'
//		         	  }*/
//		         ],
//        destroy: true,
//        ajax: {
//            url: "/openmrs/ws/rest/v1/ubs-report/getSelectedReport",
//            timeout : 300000,
//            data: function(data){
//	
//					data.startDate = "",
//					data.endDate = "",
//					data.reportName = "SP_tdh_iycf_report",
//					data.clinicCode = clinicCode,
//					data.fromAge = "0",
//					data.toAge="0"
//					
//            },
//            dataSrc: function(json){
//            	$jq('#loading_prov').hide();
//                if(json){
//                	
//                    return json;
//                }
//                else {
//                    return [];
//                }
//            },
//          
//            complete: function() {
//            	console.log("Complete report");
//            },
//            type: 'GET'
//        },
//        "language": {
//        	   "loadingRecords": "Please wait - loading..."
//        	}
//	});
//} );


$jq("#reportName").change(function (event) {
	var title = $jq('#reportName').find('option:selected').text();
	if(title == "Stock Report") {
		
//		$jq("#fromAgeBlock").hide();
//		$jq("#toAgeBlock").hide();
		$jq("#monthlyDatePicker").show();
		$jq("#normalStartDate").hide();
		$jq("#normalEndDate").hide();
		$jq("#startDate").attr("required", "false");
		$jq("#endDate").attr("required", "false");
		$jq("#startDate").prop('required',false);
		$jq("#endDate").prop('required',false);
		intializeMonthlyDatePicker();
	}
	else {
		if($jq("#monthlyDatePicker").is(":visible")){
//			$jq("#fromAgeBlock").hide();
//			$jq("#toAgeBlock").hide();
			$jq("#monthlyDatePicker").hide();
			$jq("#normalStartDate").show();
			$jq("#normalEndDate").show();
			$jq("#startDate").attr("required", "true");
			$jq("#endDate").attr("required", "true");
			$jq("#startDate").prop('required',true);
		    $jq("#endDate").prop('required',true);
			intializeNormalDatePicker();
		}
		else {
//			$jq("#fromAgeBlock").hide();
//			$jq("#toAgeBlock").hide();
			$jq("#monthlyDatePicker").hide();
			$jq("#normalStartDate").show();
			$jq("#normalEndDate").show();
			$jq("#startDate").attr("required", "true");
			$jq("#endDate").attr("required", "true");
			$jq("#startDate").prop('required',true);
		    $jq("#endDate").prop('required',true);
			intializeNormalDatePicker();
		}
	}
});


var requisitionList;
$jq("#patienterrorvisualize").on("submit",function(event){
	    event.preventDefault();
		var title = $jq('#reportName').find('option:selected').text();
		if(title == "Stock Report") {
			if($jq("#yearMonth").val() == "") {
			$jq("#dateValidation").html("This Field is Required");
				return;
			}
		}
	    $jq('#loading_prov').show();
	    var theadData = window[$jq('#reportName').val()]();
	    $jq("#thead_id").html(theadData);
	    
	    $jq('#reportTitle').html(title);
	    $jq('#divExport').css('display', 'block');
		var reportName = $jq('#reportName').val();
		var startDate = "";
		var endDate = "";
		if(title == "Stock Report") {
			var selectedDate = $jq("#yearMonth").val();
			$jq("#dateValidation").html("");
			var date = new Date(selectedDate);
            var yr = date.getFullYear();
            var  month = (date.getMonth() + 1) < 10 ? '0' + (date.getMonth() + 1) : (date.getMonth() + 1);
            var day = date.getDate() < 10 ? '0' + date.getDate() : date.getDate();
            startDate =  yr + '-' + month + "-" + day;
			var lastDateObj = new Date($jq("#yearMonth").val());
			var lastDayOfMonth = new Date(lastDateObj.getFullYear(), lastDateObj.getMonth() + 1, 0);
			var monthLast = (lastDayOfMonth.getMonth() + 1) < 10 ? '0' + (lastDayOfMonth.getMonth() + 1) : (lastDayOfMonth.getMonth() + 1);
            var dayLast = lastDayOfMonth.getDate() < 10 ? '0' + lastDayOfMonth.getDate() : lastDayOfMonth.getDate();
            endDate =  lastDateObj.getFullYear() + '-' + monthLast + "-" + dayLast; 
		}
		else {
			startDate = $jq('#startDate').val();
			endDate = $jq('#endDate').val();
		}

		var clinicCode = $jq("#clinic").val();
//		var fromAge= $jq("#fAge").val();
//		var toAge= $jq("#tAge").val();
		
 		requisitionList = $jq('#table_id').DataTable({
	        bFilter: false,
	        serverSide: false,
	        processing: true,
	        "ordering": false,
		    "searching": true,
		    /*dom: 'Bfrtip',*/
		    dom: 'frtip',
		    "pageLength": 15,
	        bInfo: false,
	        /*buttons: [
			             {
			                 extend: 'excelHtml5',
			                 title: title,
			                 text: 'Export as .xlxs'
			             }
			             ,
			             {
			         		extend: 'pdfHtml5',
			         		//title: title,
			         		text: 'Export as .pdf',
			         		orientation: 'landscape',
			         		pageSize: 'LEGAL'
			         	  }
			         ],*/
	        destroy: true,
        ajax: {
            url: "/openmrs/ws/rest/v1/ubs-report/getSelectedReport",
            timeout : 300000,
            data: function(data){
	
					data.startDate = startDate,
					data.endDate = endDate,
					data.reportName = reportName,
					data.clinicCode = clinicCode
					
            },
            dataSrc: function(json){
            	$jq('#loading_prov').hide();
                if(json){
	                var table = ""
						+ " <table id=\"table_idHidden\" class=\"table table-striped table-bordered\"> ";
					table = table+window[$jq('#reportName').val() + '_inner']();
					table = table + "<tbody>";
					for (const key in json){
						for (const value in json[key]){
							const element = json[key][value];
							table = table + "<td>" + element + "</td>";
						}
						table = table + "</tr>";
					}  
	                table=table
						+ " <tbody> </table>";
					$jq('#thead_idHidden').html(table);
                    return json;
                }
                else {
                    return [];
                }          
                
            },
            complete: function() {
            	console.log("submitted report")
            },
            type: 'GET'
            
        },
        "language": {
        	   "loadingRecords": "Please wait - loading..."
        	}
    });
});

$jq(document).ready(function() {
	$jq("#btnExport").click(function(e) {
		$jq(this).attr({
	        'download': $jq('#reportName').find('option:selected').text()+" Report.xls",
	        'href': 'data:application/csv;charset=utf-8,' + encodeURIComponent(
	            '<html  xmlns:o="urn:schemas-microsoft-com:office:office" xmlns:x="urn:schemas-microsoft-com:office:excel" xmlns="http://www.w3.org/TR/REC-html40"><head><!--[if gte mso 9]><xml><x:ExcelWorkbook><x:ExcelWorksheets><x:ExcelWorksheet><x:Name>W3C Example Table</x:Name><x:WorksheetOptions><x:DisplayGridlines/></x:WorksheetOptions></x:ExcelWorksheet></x:ExcelWorksheets></x:ExcelWorkbook></xml><![endif]--> </head>'
	            + '<body>'+ $jq('#thead_idHidden').html() +'</body> </html>'
	        )
	    })
	});
});

SP_getmonthlystockreport_inner = () => {

	var destinedThead = ""
		+ "			<thead> "
		+ "				<tr> "
		+ "					<th rowspan=\"2\">Sl.No</th> "
		+ "					<th rowspan=\"2\">Product Name</th> "
		+ "					<th rowspan=\"2\">Product Code</th> "
		+ "					<th rowspan=\"2\">Brand Name</th> "
		+ "					<th rowspan=\"2\">Category</th> "
		+ "					<th rowspan=\"2\">Packet Size</th> "
		+ "					<th rowspan=\"2\">Expire Date</th> "
		+ "					<th colspan=\"5\">Stock Information</th> "
		+ "				</tr> "
		+ "				<tr> "
		+ "					<th>Starting Balance</th> "
		+ "					<th>Out</th> "
		+ "					<th>Adjust</th> "
		+ "					<th>Stock</th> "
		+ "					<th>End Balance</th> "
		+ "				</tr> "
		+ "			</thead> ";
	
	return destinedThead;
}

SP_getmonthlystockreport = () => {

	var destinedThead = ""
		+ "<div id=\"thead_id\" style=\"overflow:auto;\"> "
		+ "		<table id=\"table_id\" class=\"table table-striped table-bordered\"> "
		+ SP_getmonthlystockreport_inner()
		+ "		</table> "
		+ "	</div>";
	
	return destinedThead;
}

SP_Prescription_inner = () => {
	var destinedThead = ""
		+ "				<thead> "
		+ "					<tr> "
		+ "						<th>Indicators</th> "
		+ "						<th>Total</th> "
		+ "						<th>Host</th> "
		+ "                     <th>FDMN</th> "
		+ "					</tr> "
		+ "				</thead> ";
	
	return destinedThead;
}

SP_Prescription = () => {
	var destinedThead = ""
		+ "<div id=\"thead_id\" style=\"overflow:auto;\"> "
		+ "			<table id=\"table_id\" class=\"table table-striped table-bordered\"> "
		+ SP_Prescription_inner()
		+ "			</table> "
		+ "			</div>";
	
	return destinedThead;
}

SP_opd_visit_report_inner = () => {
	var destinedThead = ""
		+ "				<thead> "
		+ "					<tr> "
		+ "						<th rowspan =\"2\">Indicators</th> "
		+ "						<th rowspan =\"2\">Total</th> "
		+ "						<th rowspan =\"2\">FDMN</th> "
		+ "                     <th rowspan =\"2\">Host</th> "
		+ "						<th rowspan =\"2\">Male</th> "
		+ "						<th rowspan =\"2\">Female</th> "
		+ "						<th rowspan =\"2\">Other</th> "
		+ "                     <th rowspan =\"2\">PWD</th> "
		+ "                     <th colspan =\"2\">FDMN</th> "
		+ "                     <th colspan =\"2\">Host</th> "
		+ "                     <th colspan =\"2\">&#60;5 years old</th> "
		+ "                     <th colspan =\"2\">5-17 years old</th> "
		+ "                     <th colspan =\"2\">18-59 years</th> "
		+ "                     <th colspan =\"2\">60+ years old</th> "
		+ "					</tr> "
		+ "					<tr> "
		+ "						<th>Male</th> "
		+ "						<th>Female</th> "
		+ "						<th>Male</th> "
		+ "						<th>Female</th> "
		+ "						<th>Male</th> "
		+ "						<th>Female</th> "
		+ "						<th>Male</th> "
		+ "						<th>Female</th> "
		+ "						<th>Male</th> "
		+ "						<th>Female</th> "
		+ "						<th>Male</th> "
		+ "						<th>Female</th> "
		+ "					</tr> "
		+ "				</thead> ";
	
	return destinedThead;
}

SP_opd_visit_report = () => {
	var destinedThead = ""
		+ "<div id=\"thead_id\" style=\"overflow:auto;\"> "
		+ "			<table id=\"table_id\" class=\"table table-striped table-bordered\"> "
		+ SP_opd_visit_report_inner()
		+ "			</table> "
		+ "			</div>";
	
	return destinedThead;
}

SP_nutrition_report_inner = () => {
	var destinedThead = ""
		+ "				<thead> "
		+ "					<tr> "
		+ "						<th rowspan =\"2\">Indicators</th> "
		+ "						<th rowspan =\"2\">Total</th> "
		+ "						<th rowspan =\"2\">Host</th> "
		+ "                     <th rowspan =\"2\">FDMN</th> "
		+ "						<th rowspan =\"2\">Male</th> "
		+ "						<th rowspan =\"2\">Female</th> "
		+ "                     <th rowspan =\"2\">PWD</th> "
		+ "                     <th colspan =\"2\">Host</th> "
		+ "                     <th colspan =\"2\">FDMN</th> "
		+ "					</tr> "
		+ "					<tr> "
		+ "						<th>Male</th> "
		+ "						<th>Female</th> "
		+ "						<th>Male</th> "
		+ "						<th>Female</th> "
		+ "					</tr> "
		+ "				</thead> ";
	
	return destinedThead;
}

SP_nutrition_report = () => {
	var destinedThead = ""
		+ "<div id=\"thead_id\" style=\"overflow:auto;\"> "
		+ "			<table id=\"table_id\" class=\"table table-striped table-bordered\"> "
		+ SP_nutrition_report_inner()
		+ "			</table> "
		+ "			</div>";
	
	return destinedThead;
}

SP_mhpss_report_inner = () => {
	var destinedThead = ""
		+ "				<thead> "
		+ "					<tr> "
		+ "						<th rowspan =\"2\">Indicators</th> "
		+ "						<th rowspan =\"2\">Total</th> "
		+ "						<th rowspan =\"2\">Host</th> "
		+ "                     <th rowspan =\"2\">FDMN</th> "
		+ "                     <th rowspan =\"2\">PWD</th> "
		+ "						<th rowspan =\"2\">Male</th> "
		+ "						<th rowspan =\"2\">Female</th> "
		+ "                     <th colspan =\"2\">FDMN</th> "
		+ "                     <th colspan =\"2\">Host</th> "
		+ "                     <th rowspan =\"2\">&#60;5 years old</th> "
		+ "                     <th rowspan =\"2\">5-17 years old</th> "
		+ "                     <th rowspan =\"2\">18-59 years</th> "
		+ "                     <th rowspan =\"2\">60+ years old</th> "
		+ "					</tr> "
		+ "					<tr> "
		+ "						<th>Male</th> "
		+ "						<th>Female</th> "
		+ "						<th>Male</th> "
		+ "						<th>Female</th> "
		+ "					</tr> "
		+ "				</thead> ";
	
	return destinedThead;
}

SP_mhpss_report = () => {
	var destinedThead = ""
		+ "<div id=\"thead_id\" style=\"overflow:auto;\"> "
		+ "			<table id=\"table_id\" class=\"table table-striped table-bordered\"> "
		+ SP_mhpss_report_inner()
		+ "			</table> "
		+ "			</div>";
	
	return destinedThead;
}

SP_epi_vaccine_report_inner = () => {
	var destinedThead = ""
		+ "				<thead> "
		+ "					<tr> "
		+ "						<th rowspan =\"2\">Indicators</th> "
		+ "						<th rowspan =\"2\">Total</th> "
		+ "						<th rowspan =\"2\">Host</th> "
		+ "                     <th rowspan =\"2\">FDMN</th> "
		+ "                     <th rowspan =\"2\">PWD</th> "
		+ "						<th rowspan =\"2\">Male</th> "
		+ "						<th rowspan =\"2\">Female</th> "
		+ "                     <th colspan =\"2\">FDMN</th> "
		+ "                     <th colspan =\"2\">Host</th> "
		+ "					</tr> "
		+ "					<tr> "
		+ "						<th>Male</th> "
		+ "						<th>Female</th> "
		+ "						<th>Male</th> "
		+ "						<th>Female</th> "
		+ "					</tr> "
		+ "				</thead> ";
	
	return destinedThead;
}

SP_epi_vaccine_report = () => {
	var destinedThead = ""
		+ "<div id=\"thead_id\" style=\"overflow:auto;\"> "
		+ "			<table id=\"table_id\" class=\"table table-striped table-bordered\"> "
		+ SP_epi_vaccine_report_inner()
		+ "			</table> "
		+ "			</div>";
	
	return destinedThead;
}

SP_srh_report_inner = () => {
	var destinedThead = ""
		+ "				<thead> "
		+ "					<tr> "
		+ "						<th rowspan =\"2\">Indicators</th> "
		+ "                        <th rowspan =\"2\">Total</th> "
		+ "						<th colspan =\"2\">FDMN</th> "
		+ "						<th colspan =\"2\">Host</th> "
		+ "                        <th rowspan =\"2\">PWD</th> "
		+ "                        <th rowspan =\"2\">Male</th> "
		+ "                        <th rowspan =\"2\">Female</th> "
		+ "                      "
		+ "					</tr> "
		+ "					<tr> "
		+ "						<th>Below 18</th> "
		+ "						<th>18 or Above</th> "
		+ "						<th>Below 18</th> "
		+ "						<th>18 or Above</th> "
		+ "					</tr> "
		+ "				</thead>";
	
	return destinedThead;
}

SP_srh_report = () => {
	var destinedThead = ""
		+ "<div id=\"thead_id\" style=\"overflow:auto;\"> "
		+ "			<table id=\"table_id\" class=\"table table-striped table-bordered\"> "
		+ SP_srh_report_inner()
		+ "			</table> "
		+ "			</div>";
	
	return destinedThead;
}

SP_labResult_report_inner = () => {
	var destinedThead = ""
		+ "				<thead> "
		+ "					<tr> "
		+ "						<th rowspan =\"2\">Indicators</th> "
		+ "						<th rowspan =\"2\">Total</th> "
		+ "						<th rowspan =\"2\">Male</th> "
		+ "						<th rowspan =\"2\">Female</th> "
		+ "                     <th colspan =\"2\">FDMN</th> "
		+ "                     <th colspan =\"2\">Host</th> "
		+ "                     <th rowspan =\"2\">Normal</th> "
		+ "                     <th rowspan =\"2\">Abnormal</th> "
		+ "                     <th rowspan =\"2\">Negative</th> "
		+ "                     <th rowspan =\"2\">Positive</th> "
		+ "					</tr> "
		+ "					<tr> "
		+ "						<th>Male</th> "
		+ "						<th>Female</th> "
		+ "						<th>Male</th> "
		+ "						<th>Female</th> "
		+ "					</tr> "
		+ "				</thead> ";
	
	return destinedThead;
}

SP_labResult_report = () => {
	var destinedThead = ""
		+ "<div id=\"thead_id\" style=\"overflow:auto;\"> "
		+ "			<table id=\"table_id\" class=\"table table-striped table-bordered\"> "
		+ SP_labResult_report_inner()
		+ "			</table> "
		+ "			</div>";
	
	return destinedThead;
}

SP_populationReferral_report_inner = () => {
	var destinedThead = ""
		+ "				<thead> "
		+ "					<tr> "
		+ "						<th rowspan =\"2\">Indicators</th> "
		+ "						<th rowspan =\"2\">Total</th> "
		+ "                     <th colspan =\"2\">FDMN</th> "
		+ "                     <th colspan =\"2\">Host</th> "
		+ "                     <th rowspan =\"2\">PWD</th> "
		+ "                     <th colspan =\"2\">&#60;5 years old</th> "
		+ "                     <th colspan =\"2\">5-17 years old</th> "
		+ "                     <th colspan =\"2\">18-59 years</th> "
		+ "                     <th colspan =\"2\">60+ years old</th> "
		+ "					</tr> "
		+ "					<tr> "
		+ "						<th>Male</th> "
		+ "						<th>Female</th> "
		+ "						<th>Male</th> "
		+ "						<th>Female</th> "
		+ "						<th>Male</th> "
		+ "						<th>Female</th> "
		+ "						<th>Male</th> "
		+ "						<th>Female</th> "
		+ "						<th>Male</th> "
		+ "						<th>Female</th> "
		+ "						<th>Male</th> "
		+ "						<th>Female</th> "
		+ "					</tr> "
		+ "				</thead> ";
	
	return destinedThead;
}

SP_populationReferral_report = () => {
	var destinedThead = ""
		+ "<div id=\"thead_id\" style=\"overflow:auto;\"> "
		+ "			<table id=\"table_id\" class=\"table table-striped table-bordered\"> "
		+ SP_populationReferral_report_inner()
		+ "			</table> "
		+ "			</div>";
	
	return destinedThead;
}

SP_patient_report_inner = () => {
	var destinedThead = ""
		+ "				<thead> "
		+ "					<tr> "
		+ "						<th>SN</th>"
		+ "						<th>ID</th> "
		+ "						<th>Name</th> "
		+ "						<th>FCN/Progress/NID/BRID</th> "
		+ "						<th>Gender</th> "
		+ "						<th>Birthdate</th> "
		+ "						<th>Origin</th> "
		+ "						<th>Registration Date</th> "
		+ "					</tr> "
		+ "				</thead> ";
	
	return destinedThead;
}

SP_patient_report = () => {
	var destinedThead = ""
		+ "<div id=\"thead_id\" style=\"overflow:auto;\"> "
		+ "			<table id=\"table_id\" class=\"table table-striped table-bordered\"> "
		+ SP_patient_report_inner()
		+ "			</table> "
		+ "			</div>";
	
	return destinedThead;
}


function intializeMonthlyDatePicker() {
	
		$jq('.date-picker-year').datepicker({
        changeMonth: true,
        changeYear: true,
        dateFormat: 'MM yy',
        maxDate: new Date,
        beforeShowDay:function(date){
            return [false, ''];
         },
        showButtonPanel: true
    }).focus(function() {
        var thisCalendar = $jq(this);
        //$jq('.ui-datepicker-calendar').detach();
        $jq('.ui-datepicker-close').click(function() {
            var month = $jq("#ui-datepicker-div .ui-datepicker-month :selected").val();
            var year = $jq("#ui-datepicker-div .ui-datepicker-year :selected").val();
            thisCalendar.datepicker('setDate', new Date(year, month, 1));
    		//$jq(".ui-datepicker-calendar").hide();
    		//$jq(".ui-datepicker-current").hide();
        });
    });
	$jq(".date-picker-year").focus(function () { 
		//$jq(".ui-datepicker-calendar").hide();
		//$jq(".ui-datepicker-current").hide();
    });
}


function intializeNormalDatePicker() {
		$jq("#startDate").datepicker({ dateFormat: 'yy-mm-dd', maxDate: new Date });
	$jq("#endDate").datepicker({ dateFormat: 'yy-mm-dd', minDate: new Date});
}

 
 /* $jq(document).ready( function () {
	$jq('#reportTitle').html("Acute Health Conditions");
	$jq('#table_id').DataTable({
     bFilter: false,
     serverSide: false,
     processing: true,
	    "searching": true,
		dom: 'Bfrtip',
     bInfo: true,
     destroy: true,
     ajax: {
         url: "/openmrs/ws/rest/v1/ubs-report/getSelectedReport",
         timeout : 300000,
         data: function(data){
	
					data.startDate = "",
					data.endDate = "",
					data.reportName = "SP_ubs_acute_health_condition"
					
         },
         dataSrc: function(json){
             if(json){
                 return json		;
             }
             else {
                 return [];
             }
         },
         complete: function() {
         },
         type: 'GET'
     },
		buttons: [
		             {
		                 extend: 'excelHtml5',
		                 title: "Acute Health Conditions",
		                 text: 'Export as .xlxs',
		                 customize:function(win){
		                	 var sheet = win.xl.worksheets['sheet1.xml'];
		                	 console.log(sheet);
		                	 $jq('c[r=B2 ] t', sheet).text( 'FDMN Under-5(M)' );
		                	 $jq('c[r=C2 ] t', sheet).text( 'HOST Under-5(M)' );
		                	 $jq('c[r=D2 ] t', sheet).text( 'FDMN Under-5(F)' );
		                	 $jq('c[r=E2 ] t', sheet).text( 'HOST Under-5(F)' );
		                	 $jq('c[r=F2 ] t', sheet).text( 'FDMN 6-17(M)' );
		                	 $jq('c[r=G2 ] t', sheet).text( 'HOST 6-17(M)' );
		                	 $jq('c[r=H2 ] t', sheet).text( 'FDMN 6-17(F)' );
		                	 $jq('c[r=I2 ] t', sheet).text( 'HOST 6-17(F)' );
		                	 $jq('c[r=J2 ] t', sheet).text( 'FDMN 18-49(M)' );
		                	 $jq('c[r=K2 ] t', sheet).text( 'HOST 18-49(M)' );
		                	 $jq('c[r=L2 ] t', sheet).text( 'FDMN 18-49(F)' );
		                	 $jq('c[r=M2 ] t', sheet).text( 'HOST 18-49(F)');
		                	 $jq('c[r=N2 ] t', sheet).text( 'FDMN 50 and above(M)' );
		                	 $jq('c[r=O2 ] t', sheet).text( 'HOST 50 and above(M)' );
		                	 $jq('c[r=P2 ] t', sheet).text( 'FDMN 50 and above(F)' );
		                	 $jq('c[r=Q2 ] t', sheet).text( 'HOST 50 and above(F)' );
		                	
		                	  }
		             },
		             {
			         		extend: 'pdfHtml5',
			                title: "Acute Health Conditions",
			         		text: 'Export as .pdf',
			         		orientation: 'landscape',
			         		pageSize: 'LEGAL',
			         		customize:function(pdfDocument){
			         			pdfDocument.content[1].table.headerRows = 2;
			                    var firstHeaderRow = [];
			                    $jq('#table_id').find("thead>tr:first-child>th").each(
			                            function(index, element) {
			                              var colSpan = element.getAttribute("colSpan");
			                              firstHeaderRow.push({
			                                text: element.innerHTML,
			                                style: "tableHeader",
			                                colSpan: colSpan
			                              });
			                              for (var i = 0; i < colSpan - 1; i++) {
			                                firstHeaderRow.push({});
			                              }
	                            });
			                    pdfDocument.content[1].table.body.unshift(firstHeaderRow);
			                    pdfDocument.content[1].table.body[1][0].text = "";
			                    pdfDocument.content[1].layout = "";
			                    
			         		}
				     }
		         ]
	});
} );
*/