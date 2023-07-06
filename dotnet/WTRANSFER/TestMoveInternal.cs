using WTRANSFER;

internal class TestMoveInternal
{
    public TestMoveInternal()
    {
        var siteName = "legacy-53ea73b0-cbac-4381-8427-64a53e36fc0f";
        var info = SiteInfo.GetByName(siteName);
        var fileServer = FileServer.Create(info);
        fileServer.Open();

        fileServer.TransferInternal("/Dev/usr/sap/interface/pttor/ebpp/outbound/file_example_XLSX_2MB.xlsx", "/Dev/usr/sap/interface/pttor/ebpp/outbound/Archive/file_example_XLSX_2MB.xlsx.arc");

        fileServer.Close();
    }
}