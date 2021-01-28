using System;
using System.Collections.Generic;
using System.Data.Common;
using System.Threading.Tasks;
using Xunit;

// for testcontainer
using DotNet.Testcontainers.Containers.Builders;
using DotNet.Testcontainers.Containers.Modules.Databases;
using Npgsql;
using Controller.Controllers.db;
using DotNet.Testcontainers.Containers.Configurations.Databases;

namespace Controller.Tests.Controllers.db
{
    public class PostgresFixture : IAsyncLifetime
    {

        private DbConnection _connection;
        private PostgreSqlTestcontainer _postgres;

        public async Task InitializeAsync()
        {
            //for the moment, use this testcontainer for dotnet https://github.com/HofmeisterAn/dotnet-testcontainers
            ITestcontainersBuilder<PostgreSqlTestcontainer> postgresBuilder =
                new TestcontainersBuilder<PostgreSqlTestcontainer>()
                    .WithDatabase(new PostgreSqlTestcontainerConfiguration
                    {
                        Database = "db",
                        Username = "postgres",
                        Password = "postgres",
                    })
                    .WithExposedPort(5432);
            
            _postgres = postgresBuilder.Build();
            await _postgres.StartAsync();
            _connection = new NpgsqlConnection(_postgres.ConnectionString);
            await _connection.OpenAsync();
        }

        public async Task DisposeAsync()
        {
            await _connection.CloseAsync();
            await _postgres.StopAsync();
        }

        public DbConnection GetConnection()
        {
            return _connection;
        }
    }
    
    public class DbCleanerPostgresTestBase : DbCleanTestBase, IClassFixture<PostgresFixture>, IDisposable
    {
        
        private readonly PostgresFixture _fixture;
        private readonly DbConnection _connection;
        
        public DbCleanerPostgresTestBase(PostgresFixture fixture)
        {
            _fixture = fixture;
            _connection = _fixture.GetConnection();
        }

        public void Dispose()
        {
            SqlScriptRunner.ExecCommand(_connection, "DROP SCHEMA public CASCADE;");
            SqlScriptRunner.ExecCommand(_connection, "CREATE SCHEMA public;");
            SqlScriptRunner.ExecCommand(_connection, "GRANT ALL ON SCHEMA public TO postgres;");
            SqlScriptRunner.ExecCommand(_connection, "GRANT ALL ON SCHEMA public TO public;");
        }

        protected override DbConnection GetConnection()
        {
            return _connection;
        }

        protected override SupportedDatabaseType GetDbType()
        {
            return SupportedDatabaseType.POSTGRES;
        }
    }
}