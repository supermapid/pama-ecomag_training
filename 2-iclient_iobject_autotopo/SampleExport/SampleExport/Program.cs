using System;

using SuperMap.Data;

namespace SampleExport
{
    internal class Program
    {
        public static void Main(string[] args)
        {
            var wsConnInfo = new WorkspaceConnectionInfo()
            {
                Server = @"D:\576f726b\202212001-PAMA_training\data\ecomag.smwu",
                Type = WorkspaceType.SMWU
            };
            var workspace = new Workspace();
            var wsIsOk = workspace.Open(wsConnInfo);

            if (!wsIsOk)
            {
                throw new Exception("Failed to open");
            }
            Console.WriteLine("workspace ok");

            workspace.Close();
        }
    }
}