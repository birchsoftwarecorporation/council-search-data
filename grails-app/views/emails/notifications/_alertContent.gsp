<!-- Alert -->
<table class="s-5 w-100" border="0" cellpadding="0" cellspacing="0" style="width: 100%;">
    <tbody>
    <tr>
        <td height="48" style="border-spacing: 0px; border-collapse: collapse; line-height: 48px; font-size: 48px; width: 100%; height: 48px; margin: 0;" align="left">

        </td>
    </tr>
    </tbody>
</table>

<table class="card " border="0" cellpadding="0" cellspacing="0" style="font-family: Helvetica, Arial, sans-serif; mso-table-lspace: 0pt; mso-table-rspace: 0pt; border-spacing: 0px; border-collapse: separate !important; border-radius: 4px; width: 100%; overflow: hidden; border: 1px solid #dee2e6;" bgcolor="#ffffff">
    <tbody>
    <tr>
        <td style="border-spacing: 0px; border-collapse: collapse; line-height: 24px; font-size: 16px; width: 100%; margin: 0;" align="left">
            <div>
                <table class="card-body" border="0" cellpadding="0" cellspacing="0" style="font-family: Helvetica, Arial, sans-serif; mso-table-lspace: 0pt; mso-table-rspace: 0pt; border-spacing: 0px; border-collapse: collapse; width: 100%;">
                    <tbody>
                    <tr>
                        <td style="border-spacing: 0px; border-collapse: collapse; line-height: 24px; font-size: 16px; width: 100%; margin: 0; padding: 20px;" align="left">
                            <div>
                                <!-- Alert Header -->
                                <h5 class="alertName  text-center" style="margin-top: 0; margin-bottom: 0; font-weight: bold; vertical-align: baseline; font-size: 20px; line-height: 24px;" align="center">
                                    Alert: <%= alertMap.get("name") %>
                                </h5>
                                <table class="s-2 w-100" border="0" cellpadding="0" cellspacing="0" style="width: 100%;">
                                    <tbody>
                                    <tr>
                                        <td height="8" style="border-spacing: 0px; border-collapse: collapse; line-height: 8px; font-size: 8px; width: 100%; height: 8px; margin: 0;" align="left">

                                        </td>
                                    </tr>
                                    </tbody>
                                </table>


                                <table class="container" border="0" cellpadding="0" cellspacing="0" style="font-family: Helvetica, Arial, sans-serif; mso-table-lspace: 0pt; mso-table-rspace: 0pt; border-spacing: 0px; border-collapse: collapse; width: 100%;">
                                    <tbody>
                                    <tr>
                                        <td align="center" style="border-spacing: 0px; border-collapse: collapse; line-height: 24px; font-size: 16px; margin: 0; padding: 0 16px;">
                                            <!--[if (gte mso 9)|(IE)]>
          <table align="center">
            <tbody>
              <tr>
                <td width="600">
        <![endif]-->
                                            <table align="center" border="0" cellpadding="0" cellspacing="0" style="font-family: Helvetica, Arial, sans-serif; mso-table-lspace: 0pt; mso-table-rspace: 0pt; border-spacing: 0px; border-collapse: collapse; width: 100%; max-width: 600px; margin: 0 auto;">
                                                <tbody>
                                                <tr>
                                                    <td style="border-spacing: 0px; border-collapse: collapse; line-height: 24px; font-size: 16px; margin: 0;" align="left">
                                                        <!-- Events -->
                                                        <g:each in="${alertMap.get("events")}" var="eventMap">
                                                            <g:render template="/emails/notifications/eventContent" model="[baseUrl: baseUrl, eventMap: eventMap]" />
                                                        </g:each>
                                                        <!-- End Events -->
                                                    </td>
                                                </tr>
                                                </tbody>
                                            </table>
                                            <!--[if (gte mso 9)|(IE)]>
                </td>
              </tr>
            </tbody>
          </table>
        <![endif]-->
                                        </td>
                                    </tr>
                                    </tbody>
                                </table>

                            </div>
                        </td>
                    </tr>
                    </tbody>
                </table>

            </div>
        </td>
    </tr>
    </tbody>
</table>
<table class="s-5 w-100" border="0" cellpadding="0" cellspacing="0" style="width: 100%;">
    <tbody>
    <tr>
        <td height="48" style="border-spacing: 0px; border-collapse: collapse; line-height: 48px; font-size: 48px; width: 100%; height: 48px; margin: 0;" align="left">

        </td>
    </tr>
    </tbody>
</table>



<!-- End Alert -->